/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.plugin.idgenerator.uid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an executor for padding {@link RingBuffer}<br>
 * There are two kinds of executors: one for scheduled padding, the other for padding immediately.
 */
public class BufferPaddingExecutor {

    private static final Logger logger = LogManager.getLogger(BufferPaddingExecutor.class);
    /**
     * Constants
     */
    private static final String WORKER_NAME = "RingBuffer-Padding-Worker";
    private static final String SCHEDULE_NAME = "RingBuffer-Padding-Schedule";
    private static final long DEFAULT_SCHEDULE_INTERVAL = 5 * 60L; // 5 minutes

    /**
     * Whether buffer padding is running
     */
    private final AtomicBoolean running;

    /**
     * We can borrow UIDs from the future, here store the last second we have consumed
     */
    private final PaddedAtomicLong lastSecond;

    /**
     * RingBuffer + BufferUidProvider
     */
    private final RingBuffer ringBuffer;
    private final BufferedUidProvider uidProvider;

    /**
     * Padding immediately by the thread pool
     */
    private final ExecutorService bufferPadExecutors;
    /**
     * Padding schedule thread
     */
    private final ScheduledExecutorService bufferPadSchedule;

    /**
     * Schedule interval Unit as seconds
     */
    private long scheduleInterval = DEFAULT_SCHEDULE_INTERVAL;

    /**
     * Constructor with {@link RingBuffer} and {@link BufferedUidProvider}, default use schedule
     *
     * @param ringBuffer  {@link RingBuffer}
     * @param uidProvider {@link BufferedUidProvider}
     */
    public BufferPaddingExecutor(RingBuffer ringBuffer, BufferedUidProvider uidProvider) {
        this(ringBuffer, uidProvider, true);
    }

    /**
     * Constructor with {@link RingBuffer}, {@link BufferedUidProvider}, and whether use schedule padding
     *
     * @param ringBuffer    {@link RingBuffer}
     * @param uidProvider   {@link BufferedUidProvider}
     * @param usingSchedule
     */
    public BufferPaddingExecutor(RingBuffer ringBuffer, BufferedUidProvider uidProvider, boolean usingSchedule) {
        this.running = new AtomicBoolean(false);
        this.lastSecond = new PaddedAtomicLong(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        this.ringBuffer = ringBuffer;
        this.uidProvider = uidProvider;

        // initialize thread pool
        int cores = Runtime.getRuntime().availableProcessors();
        bufferPadExecutors = Executors.newFixedThreadPool(cores * 2, new NamingThreadFactory(WORKER_NAME));

        // initialize schedule thread
        if (usingSchedule) {
            bufferPadSchedule = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory(SCHEDULE_NAME));
        } else {
            bufferPadSchedule = null;
        }
    }

    /**
     * Start executors such as schedule
     */
    public void start() {
        if (bufferPadSchedule != null) {
            bufferPadSchedule.scheduleWithFixedDelay(() -> paddingBuffer(), scheduleInterval, scheduleInterval, TimeUnit.SECONDS);
        }
    }

    /**
     * Shutdown executors
     */
    @SuppressWarnings("checkstyle:DescendantToken")
    public void shutdown() {
        if (!bufferPadExecutors.isShutdown()) {
            bufferPadExecutors.shutdownNow();
        }

        if (bufferPadSchedule != null && !bufferPadSchedule.isShutdown()) {
            bufferPadSchedule.shutdownNow();
        }
    }

    /**
     * Whether is padding
     *
     * @return
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Padding buffer in the thread pool
     */
    public void asyncPadding() {
        bufferPadExecutors.submit(this::paddingBuffer);
    }

    /**
     * Padding buffer fill the slots until to catch the cursor
     */
    @SuppressWarnings("checkstyle:DescendantToken")
    public void paddingBuffer() {
//        LOGGER.info("Ready to padding buffer lastSecond:{}. {}", lastSecond.get(), ringBuffer);

        // is still running
        if (!running.compareAndSet(false, true)) {
//            LOGGER.info("Padding buffer is still running. {}", ringBuffer);
            return;
        }

        // fill the rest slots until to catch the cursor
        boolean isFullRingBuffer = false;
        while (!isFullRingBuffer) {
            List<Long> uidList = uidProvider.provide(lastSecond.incrementAndGet());
            for (Long uid : uidList) {
                isFullRingBuffer = !ringBuffer.put(uid);
                if (isFullRingBuffer) {
                    break;
                }
            }
        }

        // not running now
        running.compareAndSet(true, false);
//        LOGGER.info("End to padding buffer lastSecond:{}. {}", lastSecond.get(), ringBuffer);
    }

    /**
     * Setters
     */
    public void setScheduleInterval(long scheduleInterval) {
//        Assert.isTrue(scheduleInterval > 0, "Schedule interval must positive!");
        if (scheduleInterval <= 0) {

        }
        this.scheduleInterval = scheduleInterval;
    }

}
