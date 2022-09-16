package com.onenine.ghmc.services;

import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.Pull;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.models.Sync;
import com.onenine.ghmc.utils.TimeUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.common.util.set.Sets;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.onenine.ghmc.services.GhPullService.GH_QUERY_DATE_FORMATTER;
import static com.onenine.ghmc.services.GhPullService.GH_QUERY_DATE_PATTERN;
import static com.onenine.ghmc.utils.TimeUtils.getCurrentUtcDateTime;
import static com.onenine.ghmc.utils.TimeUtils.getUtcDateTimeFromDate;
import static org.slf4j.LoggerFactory.getLogger;

public class PullUtils {
    private static final Logger log = getLogger(PullUtils.class);

    public static Pull getGhPull(String org, String repo, Integer number, HashSet<String> excludeCommentsFromGithubUsers, GhPullService ghPullService) throws IllegalArgumentException, ServiceException {
        if (ghPullService == null) {
            throw new IllegalArgumentException("ghPullService is required");
        }
        GHPullRequest ghPull = ghPullService.getPull(org, repo, number);
        if (ghPull == null) {
            return null;
        }
        return Pull.fromGhPullRequest(org, repo, ghPull, excludeCommentsFromGithubUsers, getCurrentUtcDateTime());
    }

    public static Pull getStoredPull(String org, String repo, Integer number, PullService pullService) throws IllegalArgumentException, ServiceException {
        if (pullService == null) {
            throw new IllegalArgumentException("pullService is required");
        }
        return pullService.getPull(org, repo, number).orElse(null);
    }

    public static List<Pull> getGhPulls(String org, String repo, HashSet<String> excludeCommentsFromGithubUsers, HashSet<String> excludeIssuesFromGithubUsers, PullState state, PullDateType dateType, String dateAfter, GhPullService ghPullService) throws IllegalArgumentException, ServiceException {
        if (ghPullService == null) {
            throw new IllegalArgumentException("ghPullService is required");
        }

        List<PullDateType> dateTypes = Lists.newArrayList();
        if (dateType == PullDateType.ALL) {
            dateTypes.add(PullDateType.CREATED);
            dateTypes.add(PullDateType.UPDATED);
            dateTypes.add(PullDateType.CLOSED);
            dateTypes.add(PullDateType.MERGED);
        } else {
            dateTypes.add(dateType);
        }

        Map<Long, GHIssue> allGhIssues = Maps.newHashMap();
        for (PullDateType activeDateType : dateTypes) {
            Instant start = Instant.now();
            log.info("Searching for {} pull requests after {} for {}/{}", activeDateType, dateAfter, org, repo);
            List<GHIssue> ghIssues = null;
            try {
                ghIssues = ghPullService.searchForPulls(org, repo, state, activeDateType, dateAfter, excludeIssuesFromGithubUsers);
            } catch (Exception e) {
                log.error("Unable to search for {} pull requests after {} for {}/{}", activeDateType, dateAfter, org, repo, e);
                continue;
            }
            Instant end = Instant.now();
            log.info("Identified {} {} pull requests after {} in {}ms", ghIssues.size(), activeDateType, dateAfter, end.toEpochMilli() - start.toEpochMilli());
            if (ghIssues != null) {
                ghIssues.stream().forEach(ghIssue -> allGhIssues.put(ghIssue.getId(), ghIssue));
            }
        }

        List<GHPullRequest> allGhPulls = null;
        log.info("Retrieving {} pull request details for {}/{}", allGhIssues.size(), org, repo);
        Instant start = Instant.now();
        try {
            allGhPulls = ghPullService.getPulls(org, repo, Lists.newArrayList(allGhIssues.values()));
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve pull requests for " + org + "/" + repo, e);
        }
        Instant end = Instant.now();
        log.info("Retrieved {} pull request details in {}ms", allGhPulls.size(), end.toEpochMilli() - start.toEpochMilli());

        if (allGhPulls == null) {
            throw new ServiceException("Unable to retrieve pull requests for " + org + "/" + repo);
        }
        if (allGhPulls.size() != allGhIssues.size()) {
            log.warn("Unable to retrieve all pull requests for {}/{} as identified via search, data is inconsistent", org, repo);
        }

        List<Pull> pulls = Lists.newArrayList();
        start = Instant.now();
        ZonedDateTime observedAt = getCurrentUtcDateTime();
        log.info("Hydrating {} pull requests for {}/{}", allGhPulls.size(), org, repo);
        int i = 0;
        for (GHPullRequest ghPullRequest : allGhPulls) {
            log.debug("Hydrating pull request {}/{} for {}/{}", ++i, allGhPulls.size(), org, repo);
            Pull pull = Pull.fromGhPullRequest(org, repo, ghPullRequest, excludeCommentsFromGithubUsers, observedAt);
            if (pull != null) {
                pulls.add(pull);
            }
        }
        end = Instant.now();
        log.info("Hydrated {} pull requests in {}ms", pulls.size(), end.toEpochMilli() - start.toEpochMilli());

        return pulls;
    }

    public static List<Pull> savePulls(String org, String repo, PullState state, PullDateType dateType, String dateAfter, List<Pull> pulls, PullService pullService, SyncService syncService) throws IllegalArgumentException, ServiceException {
        if (pullService == null) {
            throw new IllegalArgumentException("pullService is required");
        }
        if (syncService == null) {
            throw new IllegalArgumentException("syncService is required");
        }

        log.debug("Saving {} pull requests from Github for {}/{}", pulls.size(), org, repo);
        Instant saveStart = Instant.now();
        List<Pull> saved = pullService.savePulls(pulls);
        Instant saveEnd = Instant.now();
        log.debug("Completed saving {} pull requests for {}/{} in {}ms", saved != null ? saved.size() : "null", org, repo, saveEnd.toEpochMilli() - saveStart.toEpochMilli());

        List<PullDateType> dateTypes = Lists.newArrayList();
        if (dateType == PullDateType.ALL) {
            dateTypes.add(PullDateType.CREATED);
            dateTypes.add(PullDateType.UPDATED);
            dateTypes.add(PullDateType.CLOSED);
            dateTypes.add(PullDateType.MERGED);
            dateTypes.add(PullDateType.ALL);
        } else {
            dateTypes.add(dateType);
        }

        for (PullDateType activeDateType : dateTypes) {
            Sync sync = new Sync(getUtcDateTimeFromDate(saveEnd), org, repo, state, activeDateType, dateAfter);
            log.debug("Saving sync of {} pull requests from Github for {}/{}", activeDateType, org, repo);
            saveStart = Instant.now();
            try {
                syncService.saveSync(sync);
            } catch (Exception e) {
                log.warn("Unable to save sync of {} pull requests from Github for {}/{}", activeDateType, org, repo, e);
            }
            saveEnd = Instant.now();
            log.debug("Completed saving sync of {} pull requests from Github for {}/{} in {}ms", activeDateType, org, repo, saveEnd.toEpochMilli() - saveStart.toEpochMilli());
        }

        return saved;
    }

    public static List<Pull> syncPulls(
            String org,
            String repo,
            HashSet<String> excludeCommentsFromGithubUsers,
            HashSet<String> excludeIssuesFromGithubUsers,
            String aviatorMergeQueueAuthor,
            PullState state,
            PullDateType dateType,
            String dateAfter,
            Integer defaultSyncHistoryWindowDays,
            String overrideSyncHistoryWindowStartDate,
            GhPullService ghPullService,
            PullService pullService,
            SyncService syncService
    ) throws IllegalArgumentException, ServiceException {
        PullDateType activeDateType = dateType != null ? dateType : PullDateType.UPDATED;
        String activeDateAfter = null;
        if (overrideSyncHistoryWindowStartDate != null && GH_QUERY_DATE_PATTERN.matcher(overrideSyncHistoryWindowStartDate.trim()).matches()) {
            activeDateAfter = overrideSyncHistoryWindowStartDate.trim();
        } else if (dateAfter == null || dateAfter.isEmpty()) {
            Sync latestSync = syncService.getLatestSync(org, repo, state, dateType);
            if (latestSync == null && (defaultSyncHistoryWindowDays == null || defaultSyncHistoryWindowDays < 1)) {
                throw new IllegalArgumentException("No sync data available so dateAfter or defaultSyncHistoryWindowDays > 0 is required");
            } else if (latestSync != null) {
                activeDateAfter = latestSync.getTimestamp().format(GH_QUERY_DATE_FORMATTER);
            } else {
                ZonedDateTime now = TimeUtils.getCurrentUtcDateTime();
                ZonedDateTime syncAfterDefault = now.minusDays(defaultSyncHistoryWindowDays);
                activeDateAfter = syncAfterDefault.format(GH_QUERY_DATE_FORMATTER);
            }
        } else {
            activeDateAfter = dateAfter;
        }

        log.info("Syncing {}/{} pull requests on or after {} in {}/{}", state, dateType, activeDateAfter, org, repo);
        Instant startSync = Instant.now();
        log.info ("Retrieving pull requests from Github");
        Instant startRetrieve = Instant.now();
        List<Pull> pulls = PullUtils.getGhPulls(org, repo, excludeCommentsFromGithubUsers, excludeIssuesFromGithubUsers, state, activeDateType, activeDateAfter, ghPullService);
        Instant endRetrieve = Instant.now();
        log.info("Retrieved {} pull requests in {}s", pulls == null ? 0 : pulls.size(), (endRetrieve.toEpochMilli() - startRetrieve.toEpochMilli()) / 1000L);
        if (pulls == null) {
            pulls = Lists.newArrayList();
        }

        log.info("Processing {} pull requests", pulls.size());
        Instant startProcess = Instant.now();
        if (aviatorMergeQueueAuthor != null && !aviatorMergeQueueAuthor.isEmpty()) {
            pulls.forEach(pull -> AviatorUtils.updateLinkedPullRequests(pull, aviatorMergeQueueAuthor));
        }

        pulls.forEach(pull -> PullUtils.updateTimeToFirstReviewAndFeedback(pull));
        Instant endProcess = Instant.now();
        log.info("Processed {} pull requests in {}s", pulls.size(), (endProcess.toEpochMilli() - startProcess.toEpochMilli()) / 1000L);

        log.info("Saving {} pull requests", pulls.size());
        Instant startSave = Instant.now();
        List<Pull> savedPulls = PullUtils.savePulls(org, repo, state, activeDateType, activeDateAfter, pulls, pullService, syncService);
        Instant endSave = Instant.now();
        log.info("Saved {} pull requests in {}s", pulls.size(), (endSave.toEpochMilli() - startSave.toEpochMilli()) / 1000L);
        Instant endSync = Instant.now();
        log.info("Synced {} pull requests in {}s", pulls.size(), (endSync.toEpochMilli() - startSync.toEpochMilli()) / 1000L);
        return savedPulls;
    }

    public static List<PullDateType> buildPullDateTypeList(final PullDateType pullDateType) {
        List<PullDateType> pullDateTypes = Lists.newArrayList();
        if (pullDateType == null || pullDateType == PullDateType.ALL) {
            pullDateTypes.add(PullDateType.CREATED);
            pullDateTypes.add(PullDateType.UPDATED);
            pullDateTypes.add(PullDateType.CLOSED);
            pullDateTypes.add(PullDateType.MERGED);
        } else {
            pullDateTypes.add(pullDateType);
        }
        return pullDateTypes;
    }

    public static List<PullState> buildPullStateList(final PullState pullState) {
        List<PullState> pullStates = Lists.newArrayList();
        if (pullState == null || pullState == PullState.ALL) {
            pullStates.add(PullState.CLOSED);
            pullStates.add(PullState.OPEN);
        } else {
            pullStates.add(pullState);
        }
        return pullStates;
    }

    public static boolean updateTimeToFirstReviewAndFeedback(Pull pull) {
        if (pull == null) {
            return false;
        }

        ZonedDateTime firstFeedbackAt = pull.getFirstFeedbackAt();
        ZonedDateTime firstReviewAt = pull.getFirstReviewAt();
        ZonedDateTime closedAt = pull.getClosedAt();
        ZonedDateTime createdAt = pull.getCreatedAt();

        boolean updated = false;

        if (createdAt != null && firstReviewAt != null) {
            Float hoursToFirstReview = (firstReviewAt.toEpochSecond() - createdAt.toEpochSecond()) / 60.0F / 60.0F;
            pull.setHoursToFirstReview(hoursToFirstReview);
            pull.setDaysToFirstReview(hoursToFirstReview / 24.0F);
            updated = true;
        }

        if (closedAt != null && firstFeedbackAt != null) {
            Float hoursFromFirstFeedbackToClosure = (closedAt.toEpochSecond() - firstFeedbackAt.toEpochSecond()) / 60.0F / 60.0F;
            pull.setHoursFromFirstFeedbackToClosure(hoursFromFirstFeedbackToClosure);
            pull.setDaysFromFirstFeedbackToClosure(hoursFromFirstFeedbackToClosure / 24.0F);
            updated = true;
        }

        return updated;
    }

    public static boolean updateNumReviewersCommenters(Pull p) {
        if (p == null) {
            return false;
        }

        boolean updated = false;

        if (p.getReviewers() != null) {
            p.setNumReviewers(p.getReviewers().size());
            updated = true;
        }

        if (p.getApprovingReviewers() != null) {
            p.setNumApprovingReviewers(p.getApprovingReviewers().size());
            updated = true;
        }

        if (p.getRequestedReviewers() != null) {
            p.setNumRequestedReviewers(Sets.newHashSet(p.getRequestedReviewers()).size());
            updated = true;
        }

        if (p.getCommenters() != null) {
            p.setNumCommenters(p.getCommenters().size());
            updated = true;
        }

        if (p.getChangesRequestedReviewers() != null) {
            p.setNumChangesRequestedReviewers(p.getChangesRequestedReviewers().size());
            updated = true;
        }

        return updated;
    }
}
