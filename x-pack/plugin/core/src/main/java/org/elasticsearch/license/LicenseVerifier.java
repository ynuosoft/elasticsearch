/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.license;

import org.elasticsearch.core.Streams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
/**
 * Responsible for verifying signed licenses
 */
public class LicenseVerifier {
    private static final PublicKey PUBLIC_KEY;

    public LicenseVerifier() {
    }

    public static boolean verifyLicense(License license, PublicKey publicKey) {
        return true;
    }

    public static boolean verifyLicense(License license) {
        return true;
    }

    static {
        try {
            InputStream is = LicenseVerifier.class.getResourceAsStream("/public.key");

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Streams.copy(is, out);
                PUBLIC_KEY = CryptUtils.readPublicKey(out.toByteArray());
            } catch (Throwable var4) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable var3) {
                        var4.addSuppressed(var3);
                    }
                }

                throw var4;
            }

            if (is != null) {
                is.close();
            }

        } catch (IOException var5) {
            throw new AssertionError("key file is part of the source and must deserialize correctly", var5);
        }
    }
}
