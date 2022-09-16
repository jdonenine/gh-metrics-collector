package com.onenine.ghmc.services;

import com.onenine.ghmc.configuration.ApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GhClientService {
    private final ApplicationConfiguration applicationConfiguration;
    private GitHub client;

    @Autowired
    public GhClientService(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    public GitHub getClient() {
        if (client != null) {
            return client;
        }

        try {
            client = new GitHubBuilder().withOAuthToken(applicationConfiguration.getGhAccessToken()).build();
        } catch (Exception e) {
            log.error("Unable to generate Github client", e);
            return null;
        }
        return client;
    }
}
