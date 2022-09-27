package com.onenine.ghmc.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.onenine.ghmc.exceptions.DatastoreClientInitializationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;

@Component
@Slf4j
public class ElasticsearchRestClientConfiguration {
    private final ApplicationConfiguration applicationConfiguration;

    @Autowired
    public ElasticsearchRestClientConfiguration(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Bean
    public ElasticsearchClient getElasticsearchClient() throws DatastoreClientInitializationException {
        if (applicationConfiguration.getElasticsearch() == null ||
                applicationConfiguration.getElasticsearch().getHost() == null ||
                applicationConfiguration.getElasticsearch().getHost().isEmpty()) {
            throw new DatastoreClientInitializationException("elasticsearch.host is required");
        }
        if (applicationConfiguration.getElasticsearch() == null ||
                applicationConfiguration.getElasticsearch().getPort() == null ||
                applicationConfiguration.getElasticsearch().getPort() < 0) {
            throw new DatastoreClientInitializationException("elasticsearch.port is required");
        }
        String scheme = "https";
        if (applicationConfiguration.getElasticsearch() != null && !applicationConfiguration.getElasticsearch().isSsl()) {
            scheme = "http";
        }

        CredentialsProvider credentialsProvider = null;
        if (applicationConfiguration.getElasticsearch() != null && applicationConfiguration.getElasticsearch().getUsername() != null) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    applicationConfiguration.getElasticsearch().getUsername(),
                    applicationConfiguration.getElasticsearch().getPassword()
            ));
        }
        final CredentialsProvider finalCredentialsProvider = credentialsProvider;

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(
                        applicationConfiguration.getElasticsearch().getHost(),
                        applicationConfiguration.getElasticsearch().getPort(),
                        scheme
                )
        );

        SSLContext sslContext = null;
        if (applicationConfiguration.getElasticsearch().isTrustAllCertificates()) {
            try {
                sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build();
            } catch (Exception e) {
                throw new DatastoreClientInitializationException("Unable to generate SSLContext", e);
            }
        }
        final SSLContext finalSslContext = sslContext;

        builder.setHttpClientConfigCallback(
                httpClientBuilder -> {
                    if (applicationConfiguration.getElasticsearch().isDisableHostnameVerification()) {
                        httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                    }
                    if (finalSslContext != null) {
                        httpClientBuilder.setSSLContext(finalSslContext);
                    }
                    if (finalCredentialsProvider != null) {
                        httpClientBuilder.setDefaultCredentialsProvider(finalCredentialsProvider);
                    }
                    return httpClientBuilder;
                });

        ElasticsearchTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
}
