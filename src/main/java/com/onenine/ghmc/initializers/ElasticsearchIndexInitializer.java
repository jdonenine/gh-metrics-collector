package com.onenine.ghmc.initializers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.onenine.ghmc.configuration.ApplicationConfiguration;
import com.onenine.ghmc.configuration.IndexConfiguration;
import com.onenine.ghmc.configuration.TenantConfiguration;
import com.onenine.ghmc.exceptions.DatastoreClientInitializationException;
import com.onenine.ghmc.exceptions.DatastoreInitializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class ElasticsearchIndexInitializer {
    private final TenantConfiguration tenantConfiguration;
    private final ApplicationConfiguration applicationConfiguration;
    private final ElasticsearchClient client;

    @Autowired
    public ElasticsearchIndexInitializer(TenantConfiguration tenantConfiguration, ApplicationConfiguration applicationConfiguration, ElasticsearchClient client) throws DatastoreInitializationException, DatastoreClientInitializationException {
        this.tenantConfiguration = tenantConfiguration;
        this.applicationConfiguration = applicationConfiguration;
        this.client = client;
        initializeIndexes();
    }

    private void initializeIndexes() throws DatastoreInitializationException {
        Map<String, IndexConfiguration> indices = applicationConfiguration.getIndices();
        for (String index : indices.keySet()) {
            if (applicationConfiguration.getDeleteIndicesOnStartup() != null && applicationConfiguration.getDeleteIndicesOnStartup().booleanValue()) {
                deleteIndex(index);
            }
            initializeIndex(index, indices.get(index).getReplicas(), indices.get(index).getShards());
        }
    }

    private void deleteIndex(final String indexBaseName) throws DatastoreInitializationException {
        if (indexBaseName == null || indexBaseName.isEmpty()) {
            throw new DatastoreInitializationException("indexBaseName must not be null or empty");
        }

        // Index name includes the tenant-id
        final String indexName = indexBaseName.trim() + "-" + tenantConfiguration.getId();

        // Check if the index already exists
        boolean indexExists = false;
        try {
            BooleanResponse existsResponse = client.indices().exists(builder -> builder.index(indexName));
            indexExists = existsResponse.value();
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to check existence of index " + indexName, e);
        }
        if (!indexExists) {
            return;
        }

        log.info("Deleting index {}", indexName);

        try {
            DeleteIndexResponse deleteIndexResponse = client.indices().delete(builder -> builder.index(indexName));
            if (!deleteIndexResponse.acknowledged()) {
                throw new DatastoreInitializationException("Unable to delete existing index " + indexName + ", the request was not acknowledged");
            }
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to delete existing index " + indexName, e);
        }

        log.info("Deleted index {}", indexName);
    }

    private void initializeIndex(final String indexBaseName, final int numReplicas, final int numShards) throws DatastoreInitializationException {
        if (indexBaseName == null || indexBaseName.isEmpty()) {
            throw new DatastoreInitializationException("indexBaseName must not be null or empty");
        }

        // Index name includes the tenant-id
        final String indexName = indexBaseName.trim() + "-" + tenantConfiguration.getId();

        // Validate the expected mappings file
        String mappingsFileContent = null;
        final String mappingsFileName = indexBaseName.trim() + ".json";
        ClassPathResource mappingsFileResource = new ClassPathResource(mappingsFileName);
        if (!mappingsFileResource.exists()) {
            throw new DatastoreInitializationException("No mappings file provided index " + indexBaseName);
        }
        try {
            byte[] data = FileCopyUtils.copyToByteArray(mappingsFileResource.getInputStream());
            mappingsFileContent = new String(data, StandardCharsets.UTF_8);
            if (mappingsFileContent == null || mappingsFileContent.isEmpty()) {
                throw new DatastoreInitializationException("No content provided in mappings file for index " + indexBaseName);
            }
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to read mappings file for index " + indexBaseName, e);
        }

        InputStream mappingsFileInputStream;
        try {
            mappingsFileInputStream = mappingsFileResource.getInputStream();
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to establish input stream for reading of mappings file for index " + indexBaseName, e);
        }

        // Check if the index already exists
        boolean indexExists = false;
        try {
            BooleanResponse existsResponse = client.indices().exists(builder -> builder.index(indexName));
            indexExists = existsResponse.value();
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to check existence of index " + indexName, e);
        }

        // If the index does not already exist, create it
        if (!indexExists) {
            log.info("Creating index {}", indexName);

            try {
                CreateIndexResponse createIndexResponse = client.indices().create(builder -> builder
                        .index(indexName)
                        .settings(indexSettingsBuilder -> indexSettingsBuilder
                                .numberOfReplicas(String.valueOf(numReplicas))
                                .numberOfShards(String.valueOf(numShards))));
                if (!createIndexResponse.acknowledged()) {
                    throw new DatastoreInitializationException("Unable to create index " + indexName + ", the create request was not acknowledged");
                }
            } catch (Exception e) {
                throw new DatastoreInitializationException("Unable to create index " + indexName, e);
            }

            log.info("Created index {}", indexName);

        }

        // Update mappings for the index
        log.info("Updating mappings for index {}", indexName);

        try {
            PutMappingResponse putMappingResponse = client.indices().putMapping(builder -> builder
                    .index(indexName)
                    .withJson(mappingsFileInputStream)
            );
            if (!putMappingResponse.acknowledged()) {
                throw new DatastoreInitializationException("Unable to update mappings for index " + indexName + ", the put mapping request was not acknowledged");
            }
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to update mappings for index " + indexName, e);
        }

        log.info("Updated mappings for index {}", indexName);
    }
}
