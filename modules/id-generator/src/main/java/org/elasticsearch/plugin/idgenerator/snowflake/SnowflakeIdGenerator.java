/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator.snowflake;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.elasticsearch.plugin.idgenerator.IdGenerateException;
import org.elasticsearch.plugin.idgenerator.IdGenerator;
import java.text.ParseException;
import java.util.Map;

public class SnowflakeIdGenerator implements IdGenerator {

    private static final String DATE_FORMART = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    /**
     * 起始的时间戳
     */
    private long START_TIMESTAMP = 0;

    /**
     * 每一部分占用的位数
     */
    private static final long SEQUENCE_BIT = 12;   //序列号占用的位数
    private static final long MACHINE_BIT = 5;     //机器标识占用的位数
    private static final long DATA_CENTER_BIT = 5; //数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
    private static final long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private static final long MAX_DATA_CENTER_NUM = -1L ^ (-1L << DATA_CENTER_BIT);

    /**
     * 每一部分向左的位移
     */
    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;

    private long dataCenterId;  //数据中心
    private long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastTimeStamp = -1L;  //上一次时间戳

    private long getNextMill() {
        long mill = getNewTimeStamp();
        while (mill <= lastTimeStamp) {
            mill = getNewTimeStamp();
        }
        return mill;
    }

    private long getNewTimeStamp() {
        return System.currentTimeMillis();
    }

    public SnowflakeIdGenerator() {
        new SnowflakeIdGenerator(null, 0);
    }

    /**
     * @param config
     * @param defaultMachineId
     */
    public SnowflakeIdGenerator(Map<String, String> config, int defaultMachineId) {

        if (config != null) {
            String data_center_id = config.get("data_center_id");
            if (StringUtils.isEmpty(data_center_id) == false) {
                this.dataCenterId = Integer.parseInt(data_center_id);
                if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
                    throw new IllegalArgumentException("data_center_id can't be greater than MAX_DATA_CENTER_NUM or less than 0！");
                }
            }
            String machine_id = config.get("machine_id");
            if (StringUtils.isEmpty(machine_id) == false) {
                this.machineId = Integer.parseInt(machine_id);
                if (machineId > MAX_MACHINE_NUM || machineId < 0) {
                    throw new IllegalArgumentException("MachineId can't be greater than MAX_MACHINE_NUM or less than 0！");
                }
            }
            String start_timestamp = config.get("start_timestamp");
            if (StringUtils.isEmpty(start_timestamp) == false) {
                try {
                    START_TIMESTAMP = DateUtils.parseDate(start_timestamp, DATE_FORMART).getTime();
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        if (this.machineId == 0) {
            this.machineId = defaultMachineId;
        }
        if (START_TIMESTAMP <= 0) {
            START_TIMESTAMP = getNewTimeStamp() - 123;
        }

    }

    private synchronized long nextId() {
        long currTimeStamp = getNewTimeStamp();
        if (currTimeStamp < lastTimeStamp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currTimeStamp == lastTimeStamp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currTimeStamp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastTimeStamp = currTimeStamp;

        return (currTimeStamp - START_TIMESTAMP) << TIMESTAMP_LEFT //时间戳部分
            | dataCenterId << DATA_CENTER_LEFT       //数据中心部分
            | machineId << MACHINE_LEFT             //机器标识部分
            | sequence;                             //序列号部分
    }

    @Override
    public String getBase64UUID() {
        return "";
    }

    @Override
    public long getID() throws IdGenerateException {
        return nextId();
    }

    @Override
    public String parseID(long uid) {
        return "";
    }
}
