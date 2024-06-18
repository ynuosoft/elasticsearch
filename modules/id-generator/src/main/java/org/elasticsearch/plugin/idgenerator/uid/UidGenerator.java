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
import org.elasticsearch.plugin.idgenerator.IdGenerator;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class UidGenerator implements IdGenerator {

    private static final Logger logger = LogManager.getLogger(UidGenerator.class);

    /**
     * Bits allocate
     */
    protected int timeBits = 28;
    protected int workerBits = 22;
    protected int seqBits = 13;

    /**
     * Customer epoch, unit as second. For example 2016-05-20 (ms: 1463673600000)
     */
    protected String epochDate = "2024-05-01";
    protected long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(1715416045000L);

    /**
     * Stable fields after  initializing
     */
    protected BitsAllocator bitsAllocator;
    protected long workerId;

    /**
     * Volatile fields caused by nextId()
     */
    protected long sequence = 0L;
    protected long lastSecond = -1L;

    public UidGenerator() {
        new UidGenerator(null, 0);
    }

    public UidGenerator(Map<String, String> config, int defaultWorkerId) {
        // initialize bits allocator
        this.bitsAllocator = new BitsAllocator(this.timeBits, this.workerBits, this.seqBits);
        // initialize worker id
        workerId = defaultWorkerId;
        //
        if (config != null) {
            String time_bits = config.get("time_bits");
            if (StringUtils.isEmpty(time_bits) == false) {
                timeBits = Integer.parseInt(time_bits);
                if (timeBits < 0) {
                    throw new RuntimeException("timeBits " + timeBits + " must > 0 ");
                }
            }
            String worker_bits = config.get("worker_bits");
            if (StringUtils.isEmpty(worker_bits) == false) {
                workerBits = Integer.parseInt(worker_bits);
                if (workerBits < 0) {
                    throw new RuntimeException("workerBits " + workerBits + " must > 0 ");
                }
            }
            String seq_bits = config.get("seq_bits");
            if (StringUtils.isEmpty(seq_bits) == false) {
                seqBits = Integer.parseInt(seq_bits);
                if (seqBits <= 0) {
                    throw new RuntimeException("seqBits " + seqBits + " must > 0 ");
                }
            }
            String epoch_date = config.get("epoch_date");
            if (StringUtils.isEmpty(epoch_date)) {
                this.epochDate = epoch_date;
                this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(UidDateUtils.parseByDayPattern(this.epochDate).getTime());

            }
            String worker_id = config.get("worker_id");
            if (StringUtils.isEmpty(worker_id) == false) {
                workerId = Integer.parseInt(worker_id);
                //默认自动随机生成
                if (workerId == 0) {
                    workerId = defaultWorkerId;
                }
            }

        }
    }

    @Override
    public String getBase64UUID() {
        return "";
    }

    @Override
    public long getID() throws IdGenerateException {
        try {
            return nextId();
        } catch (Exception e) {
            logger.error("Generate unique id exception. ", e);
            throw new IdGenerateException(e);
        }
    }

    @Override
    public String parseID(long uid) {
        long totalBits = BitsAllocator.TOTAL_BITS;
        long signBits = bitsAllocator.getSignBits();
        long timestampBits = bitsAllocator.getTimestampBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();

        // parse UID
        long sequence = (uid << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
        long workerId = (uid << (timestampBits + signBits)) >>> (totalBits - workerIdBits);
        long deltaSeconds = uid >>> (workerIdBits + sequenceBits);

        Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
        String thatTimeStr = UidDateUtils.formatByDateTimePattern(thatTime);

        // format as string
        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
            uid, thatTimeStr, workerId, sequence);
    }

    /**
     * Get UID
     *
     * @return UID
     * @throws IdGenerateException in the case: Clock moved backwards; Exceeds the max timestamp
     */
    protected synchronized long nextId() {
        long currentSecond = getCurrentSecond();

        // Clock moved backwards, refuse to generate uid
        if (currentSecond < lastSecond) {
            long refusedSeconds = lastSecond - currentSecond;
            throw new IdGenerateException("Clock moved backwards. Refusing for %d seconds", refusedSeconds);
        }

        // At the same second, increase sequence
        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & bitsAllocator.getMaxSequence();
            // Exceed the max sequence, we wait the next second to generate uid
            if (sequence == 0) {
                currentSecond = getNextSecond(lastSecond);
            }

            // At the different second, sequence restart from zero
        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        // Allocate bits for UID
        return bitsAllocator.allocate(currentSecond - epochSeconds, workerId, sequence);
    }

    /**
     * Get next millisecond
     */
    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    /**
     * Get current second
     */
    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        if (currentSecond - epochSeconds > bitsAllocator.getMaxDeltaSeconds()) {
            throw new IdGenerateException("Timestamp bits is exhausted. Refusing UID generate. Now: " + currentSecond);
        }

        return currentSecond;
    }
}

