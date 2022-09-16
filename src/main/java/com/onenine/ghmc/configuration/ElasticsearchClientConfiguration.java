package com.onenine.ghmc.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

@Slf4j
@Configuration
public class ElasticsearchClientConfiguration extends AbstractElasticsearchConfiguration {
    private final ApplicationConfiguration applicationConfiguration;

    @Autowired
    public ElasticsearchClientConfiguration(ApplicationConfiguration applicationConfiguration) {
        super();
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public RestHighLevelClient elasticsearchClient() {
        SSLContext context = null;
        if (applicationConfiguration.getElasticsearch().isSsl() && applicationConfiguration.getElasticsearch().getCertPath() != null && !applicationConfiguration.getElasticsearch().getCertPath().isEmpty()) {
            try {
                KeyStore keyStore = KeyStore.getInstance("pkcs12");
                keyStore.load(null, null);

                FileInputStream certFileInputStream = new FileInputStream(applicationConfiguration.getElasticsearch().getCertPath());
                BufferedInputStream certFileBufferedInputStream = new BufferedInputStream(certFileInputStream);
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate = certFactory.generateCertificate(certFileBufferedInputStream);

                keyStore.setCertificateEntry(applicationConfiguration.getElasticsearch().getCertAlias(), certificate);

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                context = SSLContext.getInstance("TLS");
                context.init(null, trustManagerFactory.getTrustManagers(), null);
            } catch (Exception e) {
                log.error("Unable to initialize SSL context", e);
            }
        }

        ClientConfiguration.MaybeSecureClientConfigurationBuilder clientConfigurationBuilder = ClientConfiguration.builder()
                .connectedTo(
                        applicationConfiguration.getElasticsearch().getHost() +
                                ":" +
                                applicationConfiguration.getElasticsearch().getPort()
                );

        if (applicationConfiguration.getElasticsearch().isSsl()) {
            if (context == null) {
                clientConfigurationBuilder.usingSsl();
            } else {
                clientConfigurationBuilder
                        .usingSsl(context, new NoopHostnameVerifier());
            }
        } else {
            log.info("Generating RestClient with insecure configuration");
        }

        clientConfigurationBuilder
                .withBasicAuth(
                        applicationConfiguration.getElasticsearch().getUsername(),
                        applicationConfiguration.getElasticsearch().getPassword()
                )
                .withConnectTimeout(applicationConfiguration.getElasticsearch().getConnectionTimeoutS() * 1000L)
                .withSocketTimeout(applicationConfiguration.getElasticsearch().getSocketTimeoutS() * 1000L);
        ClientConfiguration clientConfiguration = clientConfigurationBuilder.build();

        return RestClients.create(clientConfiguration).rest();
    }
}
