package com.onenine.ghmc;

import com.onenine.ghmc.configuration.ApplicationConfiguration;
import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.Pull;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.services.GhPullService;
import com.onenine.ghmc.services.PullService;
import com.onenine.ghmc.services.PullUtils;
import com.onenine.ghmc.services.SyncService;
import com.onenine.ghmc.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
public class MetricsCollectorApplicationRunner implements ApplicationRunner {
    private final ApplicationConfiguration applicationConfiguration;
    private final PullService pullService;
    private final GhPullService ghPullService;
    private final SyncService syncService;

    @Autowired
    public MetricsCollectorApplicationRunner(
            ApplicationConfiguration applicationConfiguration,
            PullService pullService,
            GhPullService ghPullService,
            SyncService syncService
    ) {
        this.applicationConfiguration = applicationConfiguration;
        this.pullService = pullService;
        this.ghPullService = ghPullService;
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean syncOnStartup = applicationConfiguration.getSyncOnStartup() != null ? applicationConfiguration.getSyncOnStartup().booleanValue() : false;
        boolean existAfterSyncOnStartup = applicationConfiguration.getExitAfterSyncOnStartup() != null ? applicationConfiguration.getExitAfterSyncOnStartup().booleanValue() : false;

        if (!syncOnStartup) {
            return;
        }

        try {
            log.info("Starting sync of pull requests for {}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo());
            ZonedDateTime start = TimeUtils.getCurrentUtcDateTime();
            List<Pull> syncedPulls = PullUtils.syncPulls(
                    applicationConfiguration.getGhOrg(),
                    applicationConfiguration.getGhRepo(),
                    applicationConfiguration.getExcludeCommentsFromGithubUsersHashSet(),
                    applicationConfiguration.getExcludeIssuesFromGithubUsersHashSet(),
                    applicationConfiguration.getAviatorMergeQueueAuthor(),
                    PullState.ALL,
                    PullDateType.ALL,
                    null,
                    applicationConfiguration.getDefaultSyncHistoryWindowDays(),
                    applicationConfiguration.getOverrideSyncHistoryWindowStartDate(),
                    ghPullService,
                    pullService,
                    syncService);
            ZonedDateTime end = TimeUtils.getCurrentUtcDateTime();
            log.info("Synced {} pull requests for {}/{} in {}s", syncedPulls == null ? "null" : syncedPulls.size(), applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), end.toEpochSecond() - start.toEpochSecond());
        } catch (IllegalArgumentException | ServiceException e) {
            log.error("Unable to sync pull requests for {}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), e);
            if (existAfterSyncOnStartup) {
                System.exit(1);
            }
        }
        if (existAfterSyncOnStartup) {
            System.exit(0);
        }
    }
}
