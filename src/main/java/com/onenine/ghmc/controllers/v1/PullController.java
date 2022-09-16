package com.onenine.ghmc.controllers.v1;

import com.onenine.ghmc.configuration.ApplicationConfiguration;
import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.Pull;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullSource;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.services.AviatorUtils;
import com.onenine.ghmc.services.GhPullService;
import com.onenine.ghmc.services.PullService;
import com.onenine.ghmc.services.PullUtils;
import com.onenine.ghmc.services.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("api/v1/pull")
public class PullController {
    private final ApplicationConfiguration applicationConfiguration;
    private final GhPullService ghPullService;
    private final PullService pullService;
    private final SyncService syncService;

    @Autowired
    public PullController(GhPullService ghPullService, ApplicationConfiguration applicationConfiguration, PullService pullService, SyncService syncService) {
        this.ghPullService = ghPullService;
        this.applicationConfiguration = applicationConfiguration;
        this.pullService = pullService;
        this.syncService = syncService;
    }

    @GetMapping(value = "/rpc/sync", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> syncPulls(@RequestParam(required = true) PullState state, @RequestParam(required = true) PullDateType dateType, @RequestParam(required = false) String afterDate) {
        List<Pull> syncedPulls = null;
        try {
            syncedPulls = PullUtils.syncPulls(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), applicationConfiguration.getExcludeCommentsFromGithubUsersHashSet(), applicationConfiguration.getExcludeIssuesFromGithubUsersHashSet(), applicationConfiguration.getAviatorMergeQueueAuthor(), state, dateType, afterDate, applicationConfiguration.getDefaultSyncHistoryWindowDays(), null, ghPullService, pullService, syncService);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            log.error("Unable to sync pull requests for {}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(syncedPulls.stream().map(pull -> pull.getId()).collect(Collectors.toList()));
    }

    @GetMapping(value = "/rpc/link", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> syncLinkedPulls(@RequestParam(required = true) String author, @RequestParam(required = true) PullState state, @RequestParam(required = true) PullDateType dateType, @RequestParam(required = false) String afterDate) {
        Collection<Pull> syncedPulls = null;
        try {
            syncedPulls = AviatorUtils.syncLinkedPullRequests(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), author, state, dateType, afterDate, pullService);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            log.error("Unable to sync linked pull requests authored by {} for {}/{}", author, applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(syncedPulls.stream().map(pull -> pull.getId()).collect(Collectors.toList()));
    }

    @GetMapping(value = "/rpc/backfill-time-to-first-review-and-feedbackk", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> syncLinkedPulls() {
        Collection<Pull> updatedPulls = null;
        try {
            updatedPulls = pullService.backfillTimeToFirstReviewAndFeedback(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            log.error("Unable to update pull requests for {}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(updatedPulls.stream().map(pull -> pull.getId()).collect(Collectors.toList()));
    }

    @GetMapping(value = "/rpc/backfill-num-reviewers-commenters", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> backfillNumReviewersCommenters() {
        Collection<Pull> updatedPulls = null;
        try {
            updatedPulls = pullService.backfillNumReviewersCommenters(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            log.error("Unable to update pull requests for {}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(updatedPulls.stream().map(pull -> pull.getId()).collect(Collectors.toList()));
    }

    @GetMapping(value = "/{number}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Pull> getPull(@RequestParam(required = true) PullSource source, @PathVariable Integer number) {
        if (source == null) {
            return ResponseEntity.badRequest().build();
        }
        Pull pull = null;
        if (source == PullSource.GITHUB) {
            try {
                pull = PullUtils.getGhPull(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), number, applicationConfiguration.getExcludeCommentsFromGithubUsersHashSet(), ghPullService);
            } catch (ServiceException e) {
                log.error("Unable to retrieve pull request {}/{}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), number, e);
                return ResponseEntity.internalServerError().build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (source == PullSource.STORAGE) {
            try {
                pull = PullUtils.getStoredPull(applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), number, pullService);
            } catch (ServiceException e) {
                log.error("Unable to retrieve pull request {}/{}/{}", applicationConfiguration.getGhOrg(), applicationConfiguration.getGhRepo(), number, e);
                return ResponseEntity.internalServerError().build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
        if (pull == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pull);
    }

//    @GetMapping(produces = APPLICATION_JSON_VALUE)
//    public ResponseEntity<List<Pull>> getPulls(@RequestParam PullSource source, @RequestParam String afterDate, @RequestParam PullDateType dateType, @RequestParam PullState state, @RequestParam String additionalQuery) {
//        if (source == null) {
//            return ResponseEntity.badRequest().build();
//        }
//        List<Pull> pulls = null;
//        try {
//            if (source == PullSource.GITHUB) {
//                pulls = getGhPulls(applicationConfiguration.getGhOwner(), applicationConfiguration.getGhRepo(), applicationConfiguration.getExcludeCommentsFromGithubUsersHashSet(), state, dateType, afterDate, additionalQuery);
//            } else if (source == PullSource.STORAGE) {
//                return ResponseEntity.badRequest().build();
//            }
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().build();
//        } catch (ServiceException e) {
//            log.error("Unable to retrieve open pull requests", e);
//            return ResponseEntity.internalServerError().build();
//        }
//        return ResponseEntity.ok(pulls);
//    }
//
//    @GetMapping(value = "/{number}/sync", produces = APPLICATION_JSON_VALUE)
//    public ResponseEntity<Pull> syncPull(@PathVariable Integer number) {
//        Pull pull = null;
//        try {
//            pull = getGhPull(
//                    applicationConfiguration.getGhOwner(),
//                    applicationConfiguration.getGhRepo(),
//                    number,
//                    applicationConfiguration.getExcludeCommentsFromGithubUsersHashSet()
//            );
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().build();
//        } catch (ServiceException e) {
//            log.error(
//                    "Unable to retrieve pull request {}/{}/{} from Github",
//                    applicationConfiguration.getGhOwner(),
//                    applicationConfiguration.getGhRepo(),
//                    number,
//                    e
//            );
//            return ResponseEntity.internalServerError().build();
//        }
//        if (pull == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        Pull savedPull = null;
//        try {
//            savedPull = pullService.savePull(pull);
//        } catch (ServiceException e) {
//            log.error(
//                    "Unable to save pull request {}/{}/{}",
//                    applicationConfiguration.getGhOwner(),
//                    applicationConfiguration.getGhRepo(),
//                    number,
//                    e
//            );
//            return ResponseEntity.internalServerError().build();
//        }
//        return ResponseEntity.ok(savedPull);
//    }
//
//    private List<Pull> getGhPulls(String org, String repo, HashSet<String> excludeCommentsFromGithubUsers, PullState state, PullDateType dateType, String afterDate, String additionalQuery) throws IllegalArgumentException, ServiceException {
//        Instant start = Instant.now();
//        List<GHPullRequest> ghPulls = ghPullService.getPulls(org, repo, state, dateType, afterDate, additionalQuery);
//        Instant end = Instant.now();
//        log.debug("Retrieved {} pull requests in {}ms", ghPulls.size(), end.toEpochMilli() - start.toEpochMilli());
//        start = Instant.now();
//        List<Pull> pulls = ghPulls.stream().map(issue -> Pull.fromGhPullRequest(org, repo, issue, excludeCommentsFromGithubUsers)).collect(Collectors.toList());
//        end = Instant.now();
//        log.debug("Hydrated {} pull requests in {}ms", pulls.size(), end.toEpochMilli() - start.toEpochMilli());
//        return pulls;
//    }
//
//    private Pull getGhPull(String org, String repo, Integer number, HashSet<String> excludeCommentsFromGithubUsers) throws IllegalArgumentException, ServiceException {
//        Instant start = Instant.now();
//        GHPullRequest ghpr = ghPullService.getPull(org, repo, number);
//        Instant end = Instant.now();
//        log.debug("Retrieved pull request {}/{}/{} in {}ms", org, repo, number, end.toEpochMilli() - start.toEpochMilli());
//        if (ghpr == null) {
//            return null;
//        }
//        start = Instant.now();
//        Pull pull = Pull.fromGhPullRequest(org, repo, ghpr, excludeCommentsFromGithubUsers);
//        end = Instant.now();
//        log.debug("Hydrated pull request {}/{}/{} in {}ms", org, repo, number, end.toEpochMilli() - start.toEpochMilli());
//        return pull;
//    }
//
//    private Pull getStoredPull(String org, String repo, Integer number) throws IllegalArgumentException, ServiceException {
//        Optional<Pull> pull = pullService.getPull(org, repo, number);
//        return pull.orElse(null);
//    }
}
