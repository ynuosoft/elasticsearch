/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator;

public interface IdGenerator {

    String getBase64UUID();
    /**
     * Get a unique ID
     * @return UID
     * @throws IdGenerateException
     */
    long getID() throws IdGenerateException;

    /**
     * Parse the UID into elements which are used to generate the UID. <br>
     * Such as timestamp + workerId + sequence...
     * @param uid
     * @return Parsed info
     */
    String parseID(long uid);
}
