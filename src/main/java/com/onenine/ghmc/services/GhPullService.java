package com.onenine.ghmc.services;

import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueSearchBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GhPullService {
    private final GhClientService clientService;
    private final GitHub client;

    private static final String GH_QUERY_DATE_FORMAT = "\\d+\\d+\\d+\\d+\\-\\d+\\d\\-\\d+\\d";
    public static final Pattern GH_QUERY_DATE_PATTERN = Pattern.compile(GH_QUERY_DATE_FORMAT);
    public static final DateTimeFormatter GH_QUERY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC.normalized());

    @Autowired
    public GhPullService(GhClientService clientService) {
        this.clientService = clientService;
        this.client = this.clientService.getClient();
    }

    public List<GHPullRequest> getAllOpenPulls(String orgName, String repoName) throws ServiceException {
        if (orgName == null || orgName.isEmpty()) {
            throw new IllegalArgumentException("orgName is required");
        }
        if (repoName == null || repoName.isEmpty()) {
            throw new IllegalArgumentException("repoName is required");
        }

        try {
            GHOrganization org = client.getOrganization(orgName);
            GHRepository repo = org.getRepository(repoName);
            return repo.getPullRequests(GHIssueState.OPEN);
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve listing of pull requests for " + orgName + "/" + repoName, e);
        }
    }

    public GHPullRequest getPull(String orgName, String repoName, Integer number) throws IllegalArgumentException, ServiceException {
        if (orgName == null || orgName.isEmpty()) {
            throw new IllegalArgumentException("orgName is required");
        }
        if (repoName == null || repoName.isEmpty()) {
            throw new IllegalArgumentException("repoName is required");
        }
        if (number == null) {
            throw new IllegalArgumentException("number is required");
        }

        try {
            GHOrganization org = client.getOrganization(orgName);
            GHRepository repo = org.getRepository(repoName);
            return repo.getPullRequest(number);
        } catch (GHFileNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve pull request " + orgName + "/" + repoName + "/" + number, e);
        }
    }

    public List<GHIssue> searchForPulls(String org, String repo, PullState state, PullDateType dateType, String after, HashSet<String> excludeIssuesFromGithubUsers) throws IllegalArgumentException, ServiceException {
        if (dateType == null) {
            throw new IllegalArgumentException("dateType is required");
        }
        if (after == null || after.isEmpty() || GH_QUERY_DATE_PATTERN.matcher(after.trim()) == null || !GH_QUERY_DATE_PATTERN.matcher(after.trim()).matches()) {
            throw new IllegalArgumentException("after is required");
        }
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("repo is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("repo:");
        queryBuilder.append(org.trim());
        queryBuilder.append("/");
        queryBuilder.append(repo.trim());
        queryBuilder.append(" ");
        queryBuilder.append("type:pr");
        queryBuilder.append(" ");
        switch (dateType) {
            case UPDATED -> queryBuilder.append("updated:>=");
            case CLOSED -> queryBuilder.append("closed:>=");
            case MERGED -> queryBuilder.append("merged:>=");
            case CREATED -> queryBuilder.append("created:>=");
        }
        queryBuilder.append(after);
        switch (state) {
            case OPEN -> queryBuilder.append(" state:open");
            case CLOSED -> queryBuilder.append(" state:closed");
        }

        if (excludeIssuesFromGithubUsers != null && !excludeIssuesFromGithubUsers.isEmpty()) {
            for (String user : excludeIssuesFromGithubUsers) {
                queryBuilder.append(" ");
                queryBuilder.append("-author:");
                queryBuilder.append(user.trim());
            }
        }

        String query = queryBuilder.toString();

        log.debug("Executing search for issues with query {}", query);
        Instant queryStart = Instant.now();
        List<GHIssue> allIssues = Lists.newArrayList();
        try {
            GHIssueSearchBuilder searchBuilder = client
                    .searchIssues()
                    .q(query)
                    .sort(GHIssueSearchBuilder.Sort.CREATED)
                    .order(GHDirection.DESC);
            PagedSearchIterable<GHIssue> results = searchBuilder.list();
            PagedIterator<GHIssue> resultIterator = results._iterator(100);
            while (resultIterator.hasNext()) {
                List<GHIssue> page = resultIterator.nextPage();
                allIssues.addAll(page);
            }
        } catch (Exception e) {
            throw new ServiceException("Unable to search for issues with query " + query, e);
        } finally {
            Instant queryEnd = Instant.now();
            log.debug("Completed executing search for {} issues with query {} in {}ms", allIssues.size(), query, queryEnd.toEpochMilli() - queryStart.toEpochMilli());
        }

        return allIssues;
    }

    public List<GHPullRequest> getPulls(String org, String repo, List<GHIssue> issues) throws IllegalArgumentException, ServiceException {
        if (issues == null) {
            throw new IllegalArgumentException("issues is required");
        }

        GHRepository ghRepository = null;
        try {
            GHOrganization ghOrganization = client.getOrganization(org);
            ghRepository = ghOrganization.getRepository(repo);
        } catch (Exception e) {
            throw new ServiceException("Unable to access Github repository " + org + "/" + repo, e);
        }

        List<GHPullRequest> allPulls = Lists.newArrayList();
        int i = 0;
        for (GHIssue issue : issues) {
            i++;
            if (issue == null) {
                continue;
            }
            try {
                log.debug("Retrieving pull request {}/{} {}/{}/{}", i, issues.size(), org, repo, issue.getNumber());
                Instant pullStart = Instant.now();
                GHPullRequest pull = ghRepository.getPullRequest(issue.getNumber());
                Instant pullEnd = Instant.now();
                log.debug("Completed retrieving pull request {}/{} {}/{}/{} in {}ms", i, issues.size(), org, repo, issue.getNumber(), pullEnd.toEpochMilli() - pullStart.toEpochMilli());
                allPulls.add(pull);
            } catch (Exception e) {
                log.warn("Unable to retrieve pull request {}/{}/{}", org, repo, issue.getNumber(), e);
                continue;
            }
        }

        if (issues.size() != allPulls.size()) {
            log.warn("Number of pull requests retrieved ({}) does not match number of issues found ({}) via search, some data may be missing", allPulls.size(), issues.size());
        }

        return allPulls;
    }
}
