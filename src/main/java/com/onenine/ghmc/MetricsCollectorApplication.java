package com.onenine.ghmc;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class MetricsCollectorApplication {
    private static final Logger log = getLogger(MetricsCollectorApplication.class);

    public static void main(String[] args) {
        boolean syncOnStartup = false;
        boolean exitAfterSyncOnStartup = false;
        try {
            syncOnStartup = Boolean.parseBoolean(System.getenv("APP_SYNC_ON_STARTUP"));
            exitAfterSyncOnStartup = Boolean.parseBoolean(System.getenv("APP_EXIT_AFTER_SYNC_ON_STARTUP"));
        } catch (Exception e) {
            log.warn("Unable to read application environment to establish startup behavior -- starting web server by default.", e);
        }
        SpringApplication application = new SpringApplication(MetricsCollectorApplication.class);
        if (syncOnStartup && exitAfterSyncOnStartup) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }
        application.run(args);
    }

}
