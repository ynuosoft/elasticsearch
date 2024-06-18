/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.ingest.encoder.aes;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.script.ScriptService;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import static org.elasticsearch.ingest.ConfigurationUtils.newConfigurationException;

public class AesEncryptProcessor extends AbstractProcessor {
    public static final String TYPE = "aes_encrypt";
    private final List<String> fields;
    private final String targetField;
    private final ThreadLocal<Cipher> threadLocal;


    protected AesEncryptProcessor(String tag,
                                  String description,
                                  List<String> fields,
                                  String targetField,
                                  ThreadLocal<Cipher> threadLocal) {
        super(tag, description);
        this.fields = fields;
        this.targetField = targetField;
        this.threadLocal = threadLocal;
    }


    /**
     * Introspect and potentially modify the incoming data.
     *
     * @param ingestDocument
     * @return If <code>null</code> is returned then the current document will be dropped and not be indexed,
     * otherwise this document will be kept and indexed
     */
    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        var input = new StringBuilder();
        fields.forEach((fieldName) -> {
            Object fieldValue = ingestDocument.getFieldValue(fieldName, Object.class, true);
            input.append(fieldValue);
        });
        var output = threadLocal.get().doFinal(input.toString().getBytes());

        ingestDocument.setFieldValue(targetField, Base64.getEncoder().encodeToString(output));
        return super.execute(ingestDocument);
    }

    /**
     * Gets the type of a processor
     */
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTag() {
        return super.getTag();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public boolean isAsync() {
        return super.isAsync();
    }

    public static final class Factory implements Processor.Factory {

        public static final String[] SUPPORTED_AESMODE = {"AES/CBC/PKCS5Padding"};
        static final String DEFAULT_TARGET = "aes_encrypt";
        static final String DEFAULT_AES_KEY = "0123456789abcdef";
        static final String DEFAULT_AES_IV = "0123456789abcdef";

        private final ScriptService scriptService;

        public Factory(ScriptService scriptService) {
            this.scriptService = scriptService;
        }

        @Override
        public AesEncryptProcessor create(
            Map<String, Processor.Factory> registry,
            String processorTag,
            String description,
            Map<String, Object> config
        ) throws Exception {


            List<String> fields = ConfigurationUtils.readList(TYPE, processorTag, config, "fields");
            if (fields.size() < 1) {
                throw newConfigurationException(TYPE, processorTag, "fields", "must specify at least one field");
            }
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "target_field", DEFAULT_TARGET);

            String aesKey = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "aes_key", DEFAULT_AES_KEY);
            String aesIv = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "aes_iv", DEFAULT_AES_IV);

            ThreadLocal<Cipher> threadLocal = ThreadLocal.withInitial(() -> {
                try {
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(), "AES");
                    IvParameterSpec ivParameterSpec = new IvParameterSpec(aesIv.getBytes());
                    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                    return cipher;
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                    throw new IllegalStateException("unexpected exception creating MessageDigest instance for [" + e.getMessage() + "]", e);
                }
            });

            return new AesEncryptProcessor(
                processorTag,
                description,
                fields,
                targetField,
                threadLocal);
        }
    }
}
