/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator.uuid;

import org.elasticsearch.plugin.idgenerator.IdGenerateException;
import org.elasticsearch.plugin.idgenerator.IdGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class UUIDRandomGenerator implements IdGenerator {

    private static final SecureRandom INSTANCE = new SecureRandom();

    /**
     * Returns a Base64 encoded version of a Version 4.0 compatible UUID
     * as defined here: http://www.ietf.org/rfc/rfc4122.txt
     */

    @Override
    public String getBase64UUID() {
        return getBase64UUID(INSTANCE);
    }

    /**
     * Returns a Base64 encoded version of a Version 4.0 compatible UUID
     * randomly initialized by the given {@link java.util.Random} instance
     * as defined here: http://www.ietf.org/rfc/rfc4122.txt
     */
    public static String getBase64UUID(Random random) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(getUUIDBytes(random));
    }

    private static byte[] getUUIDBytes(Random random) {
        final byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        /* Set the version to version 4 (see http://www.ietf.org/rfc/rfc4122.txt)
         * The randomly or pseudo-randomly generated version.
         * The version number is in the most significant 4 bits of the time
         * stamp (bits 4 through 7 of the time_hi_and_version field).*/
        randomBytes[6] &= 0x0f; /* clear the 4 most significant bits for the version  */
        randomBytes[6] |= 0x40; /* set the version to 0100 / 0x40 */

        /* Set the variant:
         * The high field of th clock sequence multiplexed with the variant.
         * We set only the MSB of the variant*/
        randomBytes[8] &= 0x3f; /* clear the 2 most significant bits */
        randomBytes[8] |= (byte) 0x80; /* set the variant (MSB is set)*/
        return randomBytes;
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
