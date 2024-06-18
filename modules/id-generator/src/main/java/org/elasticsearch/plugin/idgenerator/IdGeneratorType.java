/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator;

import java.util.ArrayList;
import java.util.List;

public class IdGeneratorType {

    public static final List<String> LIST_ID_TYPE = new ArrayList<>();
    public static final String UID_TYPE_DEFAULT = "uid";
    public static final String UID_TYPE_CACHED = "uid_cached";
    public static final String UUID_RANDOM_TYPE = "uuid_random";
    public static final String UUID_TIME_TYPE = "uuid_time";
    public static final String LEAF_TYPE = "leaf";
    public static final String OBJECTID_TYPE = "object_id";
    public static final String SNOWFLAKE_TYPE = "snowflake";
    public static final String AUTOID_TYPE = "auto_id";

    static {
        LIST_ID_TYPE.add(UID_TYPE_DEFAULT);
        LIST_ID_TYPE.add(UID_TYPE_CACHED);
        LIST_ID_TYPE.add(UUID_RANDOM_TYPE);
        LIST_ID_TYPE.add(UUID_TIME_TYPE);
        LIST_ID_TYPE.add(LEAF_TYPE);
        LIST_ID_TYPE.add(OBJECTID_TYPE);
        LIST_ID_TYPE.add(SNOWFLAKE_TYPE);
        LIST_ID_TYPE.add(AUTOID_TYPE);
    }
}
