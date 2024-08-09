/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core;

import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.SuppressForbidden;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Information about the built version of x-pack that is running.
 */
public class XPackBuild {

    public static final XPackBuild CURRENT;

    static {
        String shortHash = "Unknown";
        String date = "Unknown";
        CURRENT = new XPackBuild(shortHash, date);
    }

    /**
     * Returns path to xpack codebase path
     */
    @SuppressForbidden(reason = "looks up path of xpack.jar directly")
    static Path getElasticsearchCodebase() {
        URL url = XPackBuild.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return PathUtils.get(url.toURI());
        } catch (URISyntaxException bogus) {
            throw new RuntimeException(bogus);
        }
    }

    private String shortHash;
    private String date;

    XPackBuild(String shortHash, String date) {
        this.shortHash = shortHash;
        this.date = date;
    }

    public String shortHash() {
        return shortHash;
    }

    public String date() {
        return date;
    }
}
