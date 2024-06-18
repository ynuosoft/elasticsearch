/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator.uid;

/**
 * If tail catches the cursor it means that the ring buffer is full, any more buffer put request will be rejected.
 * Specify the policy to handle the reject. This is a Lambda supported interface
 *
 * @author yutianbao
 */
@FunctionalInterface
public interface RejectedPutBufferHandler {

    /**
     * Reject put buffer request
     *
     * @param ringBuffer
     * @param uid
     */
    void rejectPutBuffer(RingBuffer ringBuffer, long uid);
}
