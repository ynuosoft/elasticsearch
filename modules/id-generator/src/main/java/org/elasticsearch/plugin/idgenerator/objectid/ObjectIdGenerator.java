/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator.objectid;

import org.elasticsearch.plugin.idgenerator.IdGenerateException;
import org.elasticsearch.plugin.idgenerator.IdGenerator;

public class ObjectIdGenerator implements IdGenerator {

    public ObjectIdGenerator() {

    }

    @Override
    public String getBase64UUID() {
        return "";
    }

    @Override
    public long getID() throws IdGenerateException {
        return 0;
    }

    @Override
    public String parseID(long uid) {
        return "";
    }
}
