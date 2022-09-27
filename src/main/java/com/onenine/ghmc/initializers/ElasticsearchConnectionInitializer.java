package com.onenine.ghmc.initializers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.onenine.ghmc.exceptions.DatastoreClientInitializationException;
import com.onenine.ghmc.exceptions.DatastoreInitializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ElasticsearchConnectionInitializer {
    private ElasticsearchClient client;

    @Autowired
    public ElasticsearchConnectionInitializer(ElasticsearchClient client) throws DatastoreInitializationException, DatastoreClientInitializationException {
        this.client = client;
        validateConnection();
    }

    private void validateConnection() throws DatastoreInitializationException, DatastoreClientInitializationException {
        if (this.client == null) {
            throw new DatastoreClientInitializationException("Client instance is not available.");
        }

        try {
            InfoResponse info = this.client.info();
            if (info == null) {
                throw new DatastoreInitializationException("Unable to retrieve info from cluster");
            }
            log.info("Connected to elasticsearch cluster {}({}) via node {}", info.clusterName(), info.clusterUuid(), info.name());
        } catch (Exception e) {
            throw new DatastoreInitializationException("Unable to retrieve info from cluster", e);
        }
    }
}
