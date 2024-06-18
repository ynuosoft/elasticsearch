/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.idgenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.plugin.idgenerator.objectid.ObjectIdGenerator;
import org.elasticsearch.plugin.idgenerator.snowflake.SnowflakeIdGenerator;
import org.elasticsearch.plugin.idgenerator.uid.UidGenerator;
import org.elasticsearch.plugin.idgenerator.uid.UidGeneratorCached;
import org.elasticsearch.plugin.idgenerator.uuid.UUIDRandomGenerator;
import org.elasticsearch.plugin.idgenerator.uuid.UUIDTimeGenerator;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.IngestService;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.script.ScriptService;
import java.util.Map;

public class IdGeneratorProcessor extends AbstractProcessor {

    private static final Logger logger = LogManager.getLogger(AbstractProcessor.class);

    public static final String TYPE = "id_generator";
    private final String targetField;
    private final Integer targetRadix;
    private final String targetRadixField;
    private final IdGenerator idGenerator;

    @Override
    public String getType() {
        return TYPE;
    }

    protected IdGeneratorProcessor(String tag,
                                   String description,
                                   String targetField,
                                   Integer targetRadix,
                                   String targetRadixField,
                                   IdGenerator idGenerator) {
        super(tag, description);
        this.targetField = targetField;
        this.targetRadix = targetRadix;
        this.targetRadixField = targetRadixField;
        this.idGenerator = idGenerator;
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        long targetId = idGenerator.getID();
        if (targetId > 0) {
            ingestDocument.setFieldValue(targetField, targetId);
            ingestDocument.setFieldValue(targetRadixField, Long.toString(targetId, targetRadix).toUpperCase());
        } else {
            ingestDocument.setFieldValue(targetField, idGenerator.getBase64UUID());
        }

        return super.execute(ingestDocument);
    }

    /**
     * factory
     */
    public static final class Factory implements Processor.Factory {

        static final String DEFAULT_TARGET_FIELD = "target_id";
        static final String DEFAULT_TARGET_FIELD_RADIX = "target_id_radix";
        static final Integer DEFAULT_TARGET_RADIX = 36;
        private final ScriptService scriptService;
        private final IngestService ingestService;

        public Factory(ScriptService scriptService,
                       IngestService ingestService) {
            this.scriptService = scriptService;
            this.ingestService = ingestService;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> processorFactories,
                                String tag,
                                String description,
                                Map<String, Object> config) throws Exception {

            String idType = ConfigurationUtils.readStringProperty(TYPE, tag, config,
                "type", IdGeneratorType.UID_TYPE_DEFAULT);

            String targetField = ConfigurationUtils.readStringProperty(TYPE, tag, config,
                "target_field", DEFAULT_TARGET_FIELD);

            String targetRadixField = ConfigurationUtils.readStringProperty(TYPE, tag, config,
                "target_field_radix", DEFAULT_TARGET_FIELD_RADIX);

            Integer targetRadix = ConfigurationUtils.readIntProperty(TYPE, tag, config,
                "target_radix", DEFAULT_TARGET_RADIX);
            //
            if (IdGeneratorType.LIST_ID_TYPE.contains(idType) == false) {
                throw new IdGenerateException(idType + " type not support");
            }
            IdGenerator idGeneratorLocal = null;
            //uid
            if (idType.equals(IdGeneratorType.UID_TYPE_DEFAULT)) {
                Map<String, String> uidConfig = ConfigurationUtils.readOptionalMap(TYPE, tag, config,
                    "uid_config");
                int defaultWorkerId = ingestService.getClusterService().localNode().getId().hashCode() >> 16;
                idGeneratorLocal = new UidGenerator(uidConfig, defaultWorkerId);
            } else if (idType.equals(IdGeneratorType.UID_TYPE_CACHED)) {
                Map<String, String> uidConfig = ConfigurationUtils.readOptionalMap(TYPE, tag, config,
                    "uid_config");
                int defaultWorkerId = ingestService.getClusterService().localNode().getId().hashCode() >> 16;
                idGeneratorLocal = new UidGeneratorCached(uidConfig, defaultWorkerId);
            } else if (idType.equals(IdGeneratorType.SNOWFLAKE_TYPE)) {
                Map<String, String> sfConfig = ConfigurationUtils.readOptionalMap(TYPE, tag, config,
                    "sf_config");
                int defaultMachineId = ingestService.getClusterService().localNode().getId().hashCode() >> 21;
                idGeneratorLocal = new SnowflakeIdGenerator(sfConfig, defaultMachineId);
            } else if (idType.equals(IdGeneratorType.OBJECTID_TYPE)) {
                idGeneratorLocal = new ObjectIdGenerator();
            } else if (idType.equals(IdGeneratorType.UUID_TIME_TYPE)) {
                idGeneratorLocal = new UUIDTimeGenerator();
            } else if (idType.equals(IdGeneratorType.UUID_RANDOM_TYPE)) {
                idGeneratorLocal = new UUIDRandomGenerator();
            }
            return new IdGeneratorProcessor(tag,
                description,
                targetField,
                targetRadix,
                targetRadixField,
                idGeneratorLocal);
        }
    }
}
