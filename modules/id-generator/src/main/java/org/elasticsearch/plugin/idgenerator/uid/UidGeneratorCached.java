/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator.uid;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.plugin.idgenerator.IdGenerateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class UidGeneratorCached extends UidGenerator {

    private static final Logger logger = LogManager.getLogger(UidGeneratorCached.class);

    private static final int DEFAULT_BOOST_POWER = 3;
    /**
     * Spring properties
     */
    private int boostPower = DEFAULT_BOOST_POWER;
    private int paddingFactor = RingBuffer.DEFAULT_PADDING_PERCENT;
    private Long scheduleInterval;

    private RejectedPutBufferHandler rejectedPutBufferHandler;
    private RejectedTakeBufferHandler rejectedTakeBufferHandler;

    /**
     * RingBuffer
     */
    private RingBuffer ringBuffer;
    private BufferPaddingExecutor bufferPaddingExecutor;

    public UidGeneratorCached() {
        new UidGeneratorCached(null, 0);
        this.initRingBuffer();
    }

    public UidGeneratorCached(Map<String, String> config, int defaultWorkerId) {
        super(config, defaultWorkerId);
        if (config != null) {
            String boost_power = config.get("boost_power");
            if (StringUtils.isEmpty(boost_power) == false) {
                boostPower = Integer.parseInt(boost_power);
                if (boostPower < 0) {
                    throw new RuntimeException("boostPower " + boostPower + " must > 0 ");
                }
            }
            String padding_factor = config.get("padding_factor");
            if (StringUtils.isEmpty(padding_factor) == false) {
                paddingFactor = Integer.parseInt(padding_factor);
                if (paddingFactor < 0 || paddingFactor > 100) {
                    throw new RuntimeException("paddingFactor " + paddingFactor + " must > 0 and <=100 ");
                }
            }
        }

        this.initRingBuffer();
    }

    @Override
    public long getID() {
        try {
            return ringBuffer.take();
        } catch (Exception e) {
            throw new IdGenerateException(e);
        }
    }

    @Override
    public String parseID(long uid) {
        return super.parseID(uid);
    }


    public void destroy() throws Exception {
        bufferPaddingExecutor.shutdown();
    }

    /**
     * Get the UIDs in the same specified second under the max sequence
     *
     * @param currentSecond
     * @return UID list, size of {@link BitsAllocator#getMaxSequence()} + 1
     */
    protected List<Long> nextIdsForOneSecond(long currentSecond) {
        // Initialize result list size of (max sequence + 1)
        int listSize = (int) bitsAllocator.getMaxSequence() + 1;
        List<Long> uidList = new ArrayList<>(listSize);

        // Allocate the first sequence of the second, the others can be calculated with the offset
        long firstSeqUid = bitsAllocator.allocate(currentSecond - epochSeconds, workerId, 0L);
        for (int offset = 0; offset < listSize; offset++) {
            uidList.add(firstSeqUid + offset);
        }

        return uidList;
    }

    /**
     * Initialize RingBuffer + RingBufferPaddingExecutor
     */
    private void initRingBuffer() {
        // initialize RingBuffer
        int bufferSize = ((int) bitsAllocator.getMaxSequence() + 1) << boostPower;
        this.ringBuffer = new RingBuffer(bufferSize, paddingFactor);
        logger.info("Initialized ring buffer size:{}, paddingFactor:{}", bufferSize, paddingFactor);

        // initialize RingBufferPaddingExecutor
        boolean usingSchedule = (scheduleInterval != null);
        this.bufferPaddingExecutor = new BufferPaddingExecutor(ringBuffer, this::nextIdsForOneSecond, usingSchedule);
        if (usingSchedule) {
            bufferPaddingExecutor.setScheduleInterval(scheduleInterval);
        }

        logger.info("Initialized BufferPaddingExecutor. Using schdule:{}, interval:{}", usingSchedule, scheduleInterval);

        // set rejected put/take handle policy
        this.ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
        if (rejectedPutBufferHandler != null) {
            this.ringBuffer.setRejectedPutHandler(rejectedPutBufferHandler);
        }
        if (rejectedTakeBufferHandler != null) {
            this.ringBuffer.setRejectedTakeHandler(rejectedTakeBufferHandler);
        }

        // fill in all slots of the RingBuffer
        bufferPaddingExecutor.paddingBuffer();

        // start buffer padding threads
        bufferPaddingExecutor.start();
    }

    /**
     * Setters for spring property
     */
    public void setBoostPower(int boostPower) {
//        Assert.isTrue(boostPower > 0, "Boost power must be positive!");
        if (boostPower <= 0) {
            throw new IdGenerateException("Boost power must be positive!");
        }
        this.boostPower = boostPower;
    }

    public void setRejectedPutBufferHandler(RejectedPutBufferHandler rejectedPutBufferHandler) {
        if (rejectedPutBufferHandler == null) {
            throw new IdGenerateException("RejectedPutBufferHandler can't be null!");
        }
        this.rejectedPutBufferHandler = rejectedPutBufferHandler;
    }

    public void setRejectedTakeBufferHandler(RejectedTakeBufferHandler rejectedTakeBufferHandler) {
        if (rejectedTakeBufferHandler == null) {
            throw new IdGenerateException("RejectedTakeBufferHandler can't be null!");
        }
        this.rejectedTakeBufferHandler = rejectedTakeBufferHandler;
    }

    public void setScheduleInterval(long scheduleInterval) {
        if (scheduleInterval <= 0) {
            throw new IdGenerateException("Schedule interval must positive!");
        }
        this.scheduleInterval = scheduleInterval;
    }

}

