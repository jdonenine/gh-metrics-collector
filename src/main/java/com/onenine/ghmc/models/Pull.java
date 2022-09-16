package com.onenine.ghmc.models;

import com.onenine.ghmc.services.PullUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.util.set.Sets;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.PagedIterator;
import org.slf4j.Logger;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.onenine.ghmc.utils.TimeUtils.getUtcDateTimeFromDate;
import static org.slf4j.LoggerFactory.getLogger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "pull-#{@environment.getProperty('tenant.id')}")
public class Pull {
    private static final Logger log = getLogger(Pull.class);

    private static Short test;

    @Id
    private String id;

    private String org;
    private String repo;
    private Integer number;
    private String title;
    private String author;
    private List<String> assignees;
    @Field(type = FieldType.Date)
    private ZonedDateTime createdAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime updatedAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime closedAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime mergedAt;
    private Integer numCommits;
    private Integer numComments;
    private Integer numNonReviewComments;
    private Set<String> commenters = Sets.newHashSet();
    private List<PullComment> comments = Lists.newArrayList();
    private List<PullCommit> commits = Lists.newArrayList();
    private Map<String, Integer> numCommentsByUser = Maps.newHashMap();
    private Integer numFilesChanged;
    private Integer numAdditions;
    private Integer numDeletions;
    private String htmlUrl;
    private String url;
    private String baseRef;
    private String baseSha;
    private String mergeSha;
    private Set<String> reviewers = Sets.newHashSet();
    private List<PullReview> reviews = Lists.newArrayList();
    private Set<String> approvingReviewers = Sets.newHashSet();
    private Set<String> changesRequestedReviewers = Sets.newHashSet();
    private List<String> requestedReviewers = Lists.newArrayList();
    private List<String> requestedTeams = Lists.newArrayList();
    private Integer numReviews;
    private Integer numReviewComments;
    private Integer numApprovingReviews;
    private Integer numChangesRequestedReviews;
    private Map<String, Integer> numReviewsByUser = Maps.newHashMap();
    private String mergedBy;
    private Boolean draft;
    private String state;
    private List<String> labels = Lists.newArrayList();
    private Integer numReviewers;
    private Integer numCommenters;
    private Integer numApprovingReviewers;
    private Integer numChangesRequestedReviewers;
    private Integer numRequestedReviewers;


    @Field(type = FieldType.Date)
    private ZonedDateTime firstFeedbackAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime firstCommentAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime firstReviewAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime firstApprovedReviewAt;
    private String body;

    private Float hoursToFirstApprovedReview;
    private Float daysToFirstApprovedReview;
    private Float hoursFromFirstReviewToClosure;
    private Float daysFromFirstReviewToClosure;
    private Float hoursFromFirstFeedbackToClosure;
    private Float daysFromFirstFeedbackToClosure;
    private Float hoursToFirstReview;
    private Float daysToFirstReview;
    private Float hoursToFirstFeedback;
    private Float daysToFirstFeedback;
    private Float hoursToClosure;
    private Float daysToClosure;
    @Field(type = FieldType.Date)
    private ZonedDateTime lastObservedAt;
    private Integer numCommitsAfterFirstFeedback;
    private Integer numCommitsAfterFirstReview;
    private Integer numCommitsAfterFirstApprovedReview;

    private Set<LinkedPull> linkedPullRequests = Sets.newHashSet();
    private Integer numLinkedPullRequests;
    private Set<Integer> linkedIssues = Sets.newHashSet();
    private Integer numLinkedIssues;

    public static Pull fromGhPullRequest(String org, String repo, GHPullRequest pr, HashSet<String> excludeCommentsFromGithubUsers, ZonedDateTime observedAt) {
        log.debug("Hydrating Pull from GHPullRequest {}/{}/{}", org, repo, pr.getNumber());
        Instant start = Instant.now();

        Pull p = new Pull();

        p.setLastObservedAt(observedAt);

        p.setId(org + "|" + repo + "|" + pr.getNumber());

        p.setOrg(org);

        p.setRepo(repo);

        p.setNumber(pr.getNumber());

        p.setBody(pr.getBody());

        p.setState(pr.getState() != null ? pr.getState().toString().toLowerCase() : null);

        try {
            p.setCreatedAt(getUtcDateTimeFromDate(pr.getCreatedAt()));
        } catch (Exception e) {
            log.warn("Unable to retrieve created at time for pull #{}", pr.getNumber(), e);
        }

        p.setMergedAt(pr.getMergedAt() != null ? getUtcDateTimeFromDate(pr.getMergedAt().toInstant()) : null);

        try {
            p.setUpdatedAt(pr.getUpdatedAt() != null ? getUtcDateTimeFromDate(pr.getUpdatedAt().toInstant()) : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve updatedAt for pull #{}", pr.getNumber(), e);
        }

        ZonedDateTime closedAt = pr.getClosedAt() != null ? getUtcDateTimeFromDate(pr.getClosedAt()) : null;
        ZonedDateTime firstCommentAt = null;
        ZonedDateTime firstReviewAt = null;
        ZonedDateTime firstApprovingReviewAt = null;

        Map<String, List<PullComment>> comments = null;
        try {
            comments = gatherPullComments(org, repo, pr, excludeCommentsFromGithubUsers);
        } catch (Exception e) {
            log.warn("Unable to retrieve comments for pull request {}/{}/{}", org, repo, pr.getNumber(), e);
        }

        if (comments != null) {
            List<PullComment> commentsList = buildListOfAllPullComments(comments);
            Collections.sort(commentsList);
            p.setComments(commentsList);

            PullComment firstComment = findFirstComment(comments);
            if (firstComment != null) {
                firstCommentAt = firstComment.getCreatedAt();
                p.setFirstCommentAt(firstCommentAt);
            }

            p.setNumComments(countAllComments(comments));
            p.setNumReviewComments(countReviewComments(comments));
            p.setNumNonReviewComments(p.getNumComments() - p.getNumReviewComments());
            p.setNumCommentsByUser(countAllCommentsByUser(comments));
            p.setCommenters(comments.keySet());
        }

        Map<String, List<GHPullRequestReview>> reviews = null;
        try {
            reviews = gatherPullReviews(org, repo, pr, excludeCommentsFromGithubUsers);
        } catch (Exception e) {
            log.warn("Unable to retrieve reviews for pull request {}/{}/{}", org, repo, pr.getNumber(), e);
        }
        if (reviews != null) {
            List<GHPullRequestReview> ghReviewsList = buildListOfAllPullReviews(reviews);
            List<PullReview> reviewsList = ghReviewsList.stream().map(ghReview -> PullReview.fromGHPullRequestReview(p.getOrg(), p.getRepo(), p.getNumber(), ghReview)).collect(Collectors.toList());
            Collections.sort(reviewsList);
            p.setReviews(reviewsList);

            GHPullRequestReview firstReview = findFirstReview(reviews, false);
            if (firstReview != null) {
                try {
                    firstReviewAt = getUtcDateTimeFromDate(firstReview.getSubmittedAt());
                    p.setFirstReviewAt(firstReviewAt);
                } catch (Exception e) {
                    log.warn("Unable to process first pull request review {}", firstReview.getId(), e);
                }
            }
            GHPullRequestReview firstApprovingReview = findFirstReview(reviews, true);
            if (firstApprovingReview != null) {
                try {
                    firstApprovingReviewAt = getUtcDateTimeFromDate(firstApprovingReview.getSubmittedAt());
                    p.setFirstApprovedReviewAt(firstApprovingReviewAt);
                } catch (Exception e) {
                    log.warn("Unable to process first approved pull request review {}", firstReview.getId(), e);
                }
            }
            p.setNumReviews(countAllReviews(reviews));
            p.setNumReviewsByUser(countReviewsByUser(reviews));
            p.setNumApprovingReviews(countApprovingReviews(reviews));
            p.setNumChangesRequestedReviews(countChangesRequestedReviews(reviews));
            p.setReviewers(reviews.keySet());
            p.setApprovingReviewers(findApprovingReviewers(reviews));
            p.setChangesRequestedReviewers(findChangesRequestedReviewers(reviews));
        }

        if (closedAt != null) {
            p.setClosedAt(closedAt);
            Float hoursToClosure = (p.getClosedAt().toEpochSecond() - p.getCreatedAt().toEpochSecond()) / 60.0F / 60.0F;
            p.setHoursToClosure(hoursToClosure);
            p.setDaysToClosure(p.getHoursToClosure() / 24.0F);
        }

        ZonedDateTime firstFeedbackAt = firstCommentAt;
        if (firstFeedbackAt == null) {
            if (firstReviewAt != null) {
                firstFeedbackAt = firstReviewAt;
            }
        } else {
            if (firstReviewAt != null) {
                if (firstReviewAt.isBefore(firstFeedbackAt)) {
                    firstFeedbackAt = firstReviewAt;
                }
            }
        }

        List<GHPullRequestCommitDetail> ghCommits = gatherPullCommits(org, repo, pr);
        if (ghCommits != null) {
            List<PullCommit> commits = ghCommits.stream().map(ghCommit -> PullCommit.fromGHPullRequestCommitDetail(org, repo, pr.getNumber(), ghCommit)).collect(Collectors.toList());
            Collections.sort(commits);
            p.setCommits(commits);
        }

        if (firstFeedbackAt != null) {
            p.setFirstFeedbackAt(firstFeedbackAt);
            Float hoursToFirstFeedback = (p.getFirstFeedbackAt().toEpochSecond() - p.getCreatedAt().toEpochSecond()) / 60.0F / 60.0F;
            p.setHoursToFirstFeedback(hoursToFirstFeedback);
            p.setDaysToFirstFeedback(p.getHoursToFirstFeedback() / 24.0F);

            List<PullCommit> commitsAfterFirstFeedback = p.getCommits().stream().filter(commit -> commit.getCommittedAt() != null && commit.getCommittedAt().isAfter(p.getFirstFeedbackAt())).collect(Collectors.toList());
            p.setNumCommitsAfterFirstFeedback(commitsAfterFirstFeedback.size());
        }
        if (firstApprovingReviewAt != null) {
            p.setHoursToFirstApprovedReview((p.getFirstApprovedReviewAt().toEpochSecond() - p.getCreatedAt().toEpochSecond()) / 60.0F / 60.0F);
            p.setDaysToFirstApprovedReview(p.getHoursToFirstApprovedReview() / 24.0F);

            List<PullCommit> commitsAfterFirstApprovingReview = p.getCommits().stream().filter(commit -> commit.getCommittedAt() != null && commit.getCommittedAt().isAfter(p.getFirstApprovedReviewAt())).collect(Collectors.toList());
            p.setNumCommitsAfterFirstApprovedReview(commitsAfterFirstApprovingReview.size());
        }
        if (firstReviewAt != null) {
            p.setHoursToFirstReview((p.getFirstReviewAt().toEpochSecond() - p.getCreatedAt().toEpochSecond()) / 60.0F / 60.0F);
            p.setDaysToFirstReview(p.getHoursToFirstReview() / 24.0F);

            List<PullCommit> commitsAfterFirstReview = p.getCommits().stream().filter(commit -> commit.getCommittedAt() != null && commit.getCommittedAt().isAfter(p.getFirstReviewAt())).collect(Collectors.toList());
            p.setNumCommitsAfterFirstReview(commitsAfterFirstReview.size());
        }

        try {
            p.setLabels(pr.getLabels() != null ? pr.getLabels().stream().map(label -> label.getName()).collect(Collectors.toList()) : Lists.newArrayList());
        } catch (Exception e) {
            log.warn("Unable to retrieve labels for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setDraft(pr.isDraft());
        } catch (Exception e) {
            log.warn("Unable to retrieve isDraft for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setRequestedTeams(pr.getRequestedTeams() != null ? pr.getRequestedTeams().stream().map(team -> team.getName()).collect(Collectors.toList()) : null);
        } catch (IOException e) {
            log.warn("Unable to retrieve requestedTeams for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setRequestedReviewers(pr.getRequestedReviewers() != null ? pr.getRequestedReviewers().stream().map(reviewer -> reviewer.getLogin()).collect(Collectors.toList()) : null);
        } catch (IOException e) {
            log.warn("Unable to retrieve requestedReviewers for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setMergeSha(pr.getMergeCommitSha());
        } catch (Exception e) {
            log.warn("Unable to retrieve mergeSha for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setBaseSha(pr.getBase() != null ? pr.getBase().getSha() : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve baseSha for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setBaseRef(pr.getBase() != null ? pr.getBase().getRef() : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve baseRef for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setUrl(pr.getUrl() != null ? pr.getUrl().toString() : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve url for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setHtmlUrl(pr.getHtmlUrl() != null ? pr.getHtmlUrl().toString() : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve htmlUrl for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setNumDeletions(pr.getDeletions());
        } catch (Exception e) {
            log.warn("Unable to retrieve numDeletions for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setNumAdditions(pr.getAdditions());
        } catch (Exception e) {
            log.warn("Unable to retrieve numAdditions for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setNumFilesChanged(pr.getChangedFiles());
        } catch (Exception e) {
            log.warn("Unable to retrieve numFilesChanged for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setNumCommits(pr.getCommits());
        } catch (Exception e) {
            log.warn("Unable to retrieve numCommits for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setAssignees(pr.getAssignees() != null ? pr.getAssignees().stream().map(assignee -> assignee.getLogin()).collect(Collectors.toList()) : null);
        } catch (Exception e) {
            log.warn("Unable to retrieve assignee for pull #{}", pr.getNumber(), e);
        }

        try {
            p.setAuthor(pr.getUser().getLogin());
        } catch (Exception e) {
            log.warn("Unable to retrieve user name for pull #{}", pr.getNumber(), e);
        }

        p.setTitle(pr.getTitle());

        PullUtils.updateNumReviewersCommenters(p);

        Instant end = Instant.now();
        long duration = end.toEpochMilli() - start.toEpochMilli();
        log.debug("Completed hydrating Pull from GHPullRequest {}/{}/{}, in {}ms", org, repo, pr.getNumber(), duration);

        return p;
    }

    private static Map<String, List<GHPullRequestReview>> gatherPullReviews(String org, String repo, GHPullRequest pr, HashSet<String> excludeCommentsFromGithubUsers) {
        Map<String, List<GHPullRequestReview>> map = Maps.newHashMap();
        if (pr == null) {
            return map;
        }
        PagedIterator<GHPullRequestReview> iterator = pr.listReviews().iterator();
        while (iterator.hasNext()) {
            List<GHPullRequestReview> reviews = iterator.nextPage();
            if (reviews == null) {
                continue;
            }
            for (GHPullRequestReview review : reviews) {
                try {
                    if (review == null || review.getUser() == null || review.getUser().getLogin() == null || review.getUser().getLogin().isEmpty()) {
                        continue;
                    }
                    // Do not include the author
                    if (review.getUser().getLogin().equals(pr.getUser().getLogin())) {
                        continue;
                    }
                    List<GHPullRequestReview> userReviews = map.getOrDefault(review.getUser().getLogin(), Lists.newArrayList());
                    userReviews.add(review);
                    map.put(review.getUser().getLogin(), userReviews);
                } catch (Exception e) {
                    log.warn("Unable to process review {} for pull request {}/{}/{}", review.getId(), org, repo, pr.getNumber(), e);
                    continue;
                }
            }
        }
        return map;
    }

    private static Set<String> findApprovingReviewers(Map<String, List<GHPullRequestReview>> reviews) {
        Set<String> approvers = Sets.newHashSet();
        if (reviews == null || reviews.isEmpty()) {
            return approvers;
        }
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews == null || userReviews.isEmpty()) {
                continue;
            }
            for (GHPullRequestReview review : userReviews) {
                if (review == null || review.getState() == null || review.getState() != GHPullRequestReviewState.APPROVED) {
                    continue;
                }
                approvers.add(user);
                break;
            }
        }
        return approvers;
    }

    private static Set<String> findChangesRequestedReviewers(Map<String, List<GHPullRequestReview>> reviews) {
        Set<String> approvers = Sets.newHashSet();
        if (reviews == null || reviews.isEmpty()) {
            return approvers;
        }
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews == null || userReviews.isEmpty()) {
                continue;
            }
            for (GHPullRequestReview review : userReviews) {
                if (review == null || review.getState() == null || review.getState() != GHPullRequestReviewState.CHANGES_REQUESTED) {
                    continue;
                }
                approvers.add(user);
                break;
            }
        }
        return approvers;
    }

    private static Map<String, Integer> countReviewsByUser(Map<String, List<GHPullRequestReview>> reviews) {
        Map<String, Integer> map = Maps.newHashMap();
        if (reviews == null || reviews.isEmpty()) {
            return map;
        }
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReview = reviews.get(user);
            if (userReview != null) {
                map.put(user, userReview.size());
            }
        }
        return map;
    }

    private static Integer countChangesRequestedReviews(Map<String, List<GHPullRequestReview>> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews == null || userReviews.isEmpty()) {
                continue;
            }
            for (GHPullRequestReview review : userReviews) {
                if (review == null || review.getState() == null || review.getState() != GHPullRequestReviewState.CHANGES_REQUESTED) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    private static Integer countApprovingReviews(Map<String, List<GHPullRequestReview>> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews == null || userReviews.isEmpty()) {
                continue;
            }
            for (GHPullRequestReview review : userReviews) {
                if (review == null || review.getState() == null || review.getState() != GHPullRequestReviewState.APPROVED) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    private static Integer countAllReviews(Map<String, List<GHPullRequestReview>> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews != null) {
                count += userReviews.size();
            }
        }
        return count;
    }

    private static List<PullComment> buildListOfAllPullComments(Map<String, List<PullComment>> comments) {
        List<PullComment> allComments = Lists.newArrayList();
        comments.values().stream()
                .filter(userComments -> userComments != null && !userComments.isEmpty())
                .forEach(userComments -> allComments.addAll(userComments));
        return allComments;
    }

    private static Map<String, Integer> countAllCommentsByUser(Map<String, List<PullComment>> comments) {
        Map<String, Integer> map = Maps.newHashMap();
        if (comments == null || comments.isEmpty()) {
            return map;
        }
        for (String user : comments.keySet()) {
            List<PullComment> userComments = comments.get(user);
            if (userComments != null) {
                map.put(user, userComments.size());
            }
        }
        return map;
    }

    private static int countAllComments(Map<String, List<PullComment>> comments) {
        if (comments == null || comments.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String user : comments.keySet()) {
            List<PullComment> userComments = comments.get(user);
            if (userComments != null) {
                count += userComments.size();
            }
        }
        return count;
    }

    private static int countReviewComments(Map<String, List<PullComment>> comments) {
        if (comments == null || comments.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String user : comments.keySet()) {
            List<PullComment> userComments = comments.get(user);
            if (userComments == null || userComments.isEmpty()) {
                continue;
            }
            for (PullComment comment : userComments) {
                if (comment == null || !comment.isReviewComment()) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    private static List<GHPullRequestReview> buildListOfAllPullReviews(Map<String, List<GHPullRequestReview>> reviews) {
        List<GHPullRequestReview> allReviews = Lists.newArrayList();
        reviews.values().stream()
                .filter(userReviews -> userReviews != null && !userReviews.isEmpty())
                .forEach(userReviews -> allReviews.addAll(userReviews));
        return allReviews;
    }

    private static GHPullRequestReview findFirstReview(Map<String, List<GHPullRequestReview>> reviews, boolean onlyApproved) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }
        GHPullRequestReview firstReview = null;
        for (String user : reviews.keySet()) {
            List<GHPullRequestReview> userReviews = reviews.get(user);
            if (userReviews == null || userReviews.isEmpty()) {
                continue;
            }
            for (GHPullRequestReview review : userReviews) {
                try {
                    if (review == null || review.getSubmittedAt() == null) {
                        continue;
                    }
                    if (onlyApproved && (review.getState() == null || review.getState() != GHPullRequestReviewState.APPROVED)) {
                        continue;
                    }
                    if (firstReview == null) {
                        firstReview = review;
                    } else if (firstReview.getSubmittedAt().toInstant().isAfter(review.getSubmittedAt().toInstant())) {
                        firstReview = review;
                    }
                } catch (Exception e) {
                    log.warn("Unable to process review {}", review.getId(), e);
                }
            }
        }
        return firstReview;
    }

    private static PullComment findFirstComment(Map<String, List<PullComment>> comments) {
        if (comments == null || comments.isEmpty()) {
            return null;
        }
        PullComment firstComment = null;
        for (String user : comments.keySet()) {
            List<PullComment> userComments = comments.get(user);
            if (userComments == null || userComments.isEmpty()) {
                continue;
            }
            for (PullComment comment : userComments) {
                if (comment == null || comment.getCreatedAt() == null) {
                    continue;
                }
                if (firstComment == null) {
                    firstComment = comment;
                } else if (firstComment.getCreatedAt().isAfter(comment.getCreatedAt())) {
                    firstComment = comment;
                }
            }
        }
        return firstComment;
    }

    private static List<GHPullRequestCommitDetail> gatherPullCommits(String org, String repo, GHPullRequest pr) {
        List<GHPullRequestCommitDetail> allCommits = null;
        try {
            PagedIterator<GHPullRequestCommitDetail> iterator = pr.listCommits().iterator();
            while (iterator.hasNext()) {
                List<GHPullRequestCommitDetail> page = iterator.nextPage();
                if (page != null) {
                    if (allCommits == null) {
                        allCommits = Lists.newArrayList();
                        allCommits.addAll(page);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to get commits from pull {}/{}/{}", org, repo, pr.getNumber(), e);
        }
        return allCommits;
    }

    private static Map<String, List<PullComment>> gatherPullComments(String org, String repo, GHPullRequest pr, HashSet<String> excludeCommentsFromUsers) {
        Map<String, List<PullComment>> map = Maps.newHashMap();
        if (pr == null) {
            return map;
        }

        try {
            for (GHIssueComment comment : pr.getComments()) {
                if (comment == null || comment.getUser() == null || comment.getUser().getLogin() == null || comment.getUser().getLogin().isEmpty()) {
                    continue;
                }
                if (excludeCommentsFromUsers != null && excludeCommentsFromUsers.contains(comment.getUser().getLogin())) {
                    continue;
                }
                List<PullComment> comments = map.getOrDefault(comment.getUser().getLogin(), Lists.newArrayList());
                comments.add(new PullComment(org, repo, pr.getNumber(), comment.getUser().getLogin(), getUtcDateTimeFromDate(comment.getCreatedAt()), false));
                map.put(comment.getUser().getLogin(), comments);
            }
        } catch (Exception e) {
            log.warn("Unable to get comments from pull request {}/{}/{}", org, repo, pr.getNumber(), e);
        }

        try {
            PagedIterator<GHPullRequestReviewComment> iterator = pr.listReviewComments().iterator();
            while (iterator.hasNext()) {
                for (GHPullRequestReviewComment comment : iterator.nextPage()) {
                    if (comment == null || comment.getUser() == null || comment.getUser().getLogin() == null || comment.getUser().getLogin().isEmpty()) {
                        continue;
                    }
                    if (excludeCommentsFromUsers != null && excludeCommentsFromUsers.contains(comment.getUser().getLogin())) {
                        continue;
                    }
                    List<PullComment> comments = map.getOrDefault(comment.getUser().getLogin(), Lists.newArrayList());
                    comments.add(new PullComment(org, repo, pr.getNumber(), comment.getUser().getLogin(), getUtcDateTimeFromDate(comment.getCreatedAt()), true));
                    map.put(comment.getUser().getLogin(), comments);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to get review comments from pull {}/{}/{}", org, repo, pr.getNumber(), e);
        }

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pull pull = (Pull) o;
        return id.equals(pull.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
