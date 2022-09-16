package com.onenine.ghmc.services;

import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.LinkedPull;
import com.onenine.ghmc.models.LinkedPullType;
import com.onenine.ghmc.models.Pull;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class AviatorUtils {
    private static final String linkedPRMainLineRegex = "^PR #(\\d+) will be merged after CI of this PR completes\\.$";
    private static final String linkedPRMainLineRegex2 = "^This PR contains changes from PR #(\\d+). This PR will be fast-forwarded into \\S+ \\(and the original PR will be closed\\) when CI passes\\.$";
    private static final Pattern linkedPRMainLinePattern = Pattern.compile(linkedPRMainLineRegex, Pattern.CASE_INSENSITIVE);
    private static final Pattern linkedPRMainLinePattern2 = Pattern.compile(linkedPRMainLineRegex2, Pattern.CASE_INSENSITIVE);
    private static final String linkedPRListStartLineRegex = "This PR also includes changes from the following PRs:";
    private static final Pattern linkedPRListStartLinePattern = Pattern.compile(linkedPRListStartLineRegex, Pattern.CASE_INSENSITIVE);
    private static final String linkedPRListLineRegex = "^(\\d+)$";
    private static final Pattern linkedPRListLineRegexPattern = Pattern.compile(linkedPRListLineRegex, Pattern.CASE_INSENSITIVE);

    private static final Logger log = getLogger(AviatorUtils.class);

    public static boolean updateLinkedPullRequests(Pull pull, String aviatorMergeQueueAuthor) {
        if (pull == null) {
            return false;
        }
        if (aviatorMergeQueueAuthor == null || aviatorMergeQueueAuthor.isEmpty()) {
            log.warn("Unable to process pull request avaiatorMergeQueueAuthor is invalid");
            return false;
        }
        if (pull.getAuthor() == null || !pull.getAuthor().equalsIgnoreCase(aviatorMergeQueueAuthor)) {
            return false;
        }
        if (pull.getBody() == null || pull.getBody().isEmpty()) {
            log.warn("Unable to process pull request {}/{}/{} for author {} body is empty", pull.getOrg(), pull.getRepo(), pull.getNumber(), aviatorMergeQueueAuthor);
            return false;
        }

        String body = pull.getBody();
        body = body.replaceAll("\\r", "");
        String[] lines = body.split("\n");
        if (lines == null) {
            log.warn("Unable to process pull request {}/{}/{} for author {} body could not be parsed", pull.getOrg(), pull.getRepo(), pull.getNumber(), aviatorMergeQueueAuthor);
            return false;
        }

        HashSet<Integer> linkedPullNumbers = Sets.newHashSet();

        boolean listingPrs = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) {
                continue;
            }

            Matcher mainLineMatcher = linkedPRMainLinePattern.matcher(line.trim());
            Matcher mainLineMatcher2 = linkedPRMainLinePattern2.matcher(line.trim());
            Matcher matchedMainLineMatcher = null;
            if (mainLineMatcher != null && mainLineMatcher.find()) {
                matchedMainLineMatcher = mainLineMatcher;
            } else if (mainLineMatcher2 != null && mainLineMatcher2.find()) {
                matchedMainLineMatcher = mainLineMatcher2;
            }
            if (matchedMainLineMatcher != null) {
                String value = matchedMainLineMatcher.group(1);
                Integer number = null;
                try {
                    number = Integer.parseInt(value);
                } catch (Exception e) {
                    number = null;
                }
                if (number != null) {
                    linkedPullNumbers.add(number);
                }
                continue;
            } else if (i == 0) {
                log.warn("Unable to parse first line of body for pull request {}/{}/{} as expected: {}", pull.getOrg(), pull.getRepo(), pull.getNumber(), line);
            }

            Matcher listStartLineMatcher = linkedPRListStartLinePattern.matcher(line.trim());
            if (listStartLineMatcher != null && listStartLineMatcher.find()) {
                listingPrs = true;
                continue;
            }

            Matcher listLineMatcher = linkedPRListLineRegexPattern.matcher(line.trim());
            if (listingPrs && listLineMatcher != null && listLineMatcher.find()) {
                String value = listLineMatcher.group(1);
                Integer number = null;
                try {
                    number = Integer.parseInt(value);
                } catch (Exception e) {
                    number = null;
                }
                if (number != null) {
                    linkedPullNumbers.add(number);
                }
                continue;
            }
        }

        if (linkedPullNumbers != null) {
            pull.setLinkedPullRequests(linkedPullNumbers.stream().map(number -> new LinkedPull(pull.getOrg(), pull.getRepo(), number, LinkedPullType.AVIATOR_MERGE_QUEUE)).collect(Collectors.toSet()));
            pull.setNumLinkedPullRequests(linkedPullNumbers.size());
            return true;
        }
        return false;
    }

    public static Collection<Pull> syncLinkedPullRequests(String org, String repo, String aviatorMergeQueueAuthor, PullState state, PullDateType pullDateType, String dateAfter, PullService pullService) throws IllegalArgumentException, ServiceException {
        if (pullService == null) {
            throw new IllegalArgumentException("pullService is required");
        }

        // Find all of the PRs authored by the MergeQueue bot
        Collection<Pull> pulls = pullService.getPullsByAuthor(org, repo, aviatorMergeQueueAuthor, state, pullDateType, dateAfter);
        if (pulls == null) {
            log.warn("Retrieved not pull requests authored by {} for {}/{}, no sync completed", aviatorMergeQueueAuthor, org, repo);
            return Lists.newArrayList();
        }

        if (pulls == null || pulls.isEmpty()) {
            return Lists.newArrayList();
        }

        // Process each PR to parse the content of the PR for the list of the linked PRs
        List<Pull> updatedPulls = Lists.newArrayList();

        for (Pull pull : pulls) {
            if (updateLinkedPullRequests(pull, aviatorMergeQueueAuthor)) {
                updatedPulls.add(pull);
            }
        }

        if (updatedPulls != null && !updatedPulls.isEmpty()) {
            try {
                pullService.savePulls(updatedPulls);
            } catch (Exception e) {
                throw new ServiceException("Unable to save updated pull requests by author " + aviatorMergeQueueAuthor + " for " + org + "/" + repo, e);
            }
        }
        return updatedPulls;
    }
}
