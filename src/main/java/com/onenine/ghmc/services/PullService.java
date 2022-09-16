package com.onenine.ghmc.services;

import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.Pull;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.repositories.PullRepository;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class PullService {
    private final PullRepository pullRepository;

    @Autowired
    public PullService(PullRepository pullRepository) {
        this.pullRepository = pullRepository;
    }

    public Optional<Pull> getPull(String org, String repo, Integer number) throws IllegalArgumentException, ServiceException {
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("repo is required");
        }
        if (number == null) {
            throw new IllegalArgumentException("number is required");
        }
        try {
            return pullRepository.findByOrgAndRepoAndNumber(org.trim(), repo.trim(), number);
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve pull " + org.trim() + "/" + repo.trim() + "/" + number, e);
        }
    }

    public Pull savePull(Pull pull) throws ServiceException {
        if (pull == null) {
            throw new IllegalArgumentException("pull is required");
        }

        Pull saved = null;
        try {
            saved = pullRepository.save(pull);
        } catch (Exception e) {
            log.error("Unable to save pull", e);
            throw new ServiceException("Unable to save pull", e);
        }
        return saved;
    }

    public List<Pull> savePulls(List<Pull> pulls) throws ServiceException {
        if (pulls == null) {
            throw new IllegalArgumentException("pulls is required");
        }

        Iterable<Pull> saved = null;
        try {
            saved = pullRepository.saveAll(pulls);
        } catch (Exception e) {
            log.error("Unable to save pulls", e);
            throw new ServiceException("Unable to save pulls", e);
        }
        return Lists.newArrayList(saved);
    }

    public List<Pull> getAllPulls(String org, String repo, int pageSize) throws IllegalArgumentException, ServiceException {
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("repo is required");
        }
        if (pageSize <= 0 || pageSize > 1000) {
            throw new IllegalArgumentException("pageSize must be > 0 and <= 1000");
        }

        List<Pull> allPulls = Lists.newArrayList();
        try {
            Page<Pull> page = pullRepository.findByOrgAndRepoOrderByCreatedAt(org, repo, Pageable.ofSize(pageSize));
            while (page != null && page.hasNext()) {
                allPulls.addAll(page.toList());
                page = pullRepository.findAll(page.nextPageable());
            }
            if (page != null) {
                allPulls.addAll(page.toList());
            }
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve pulls to update");
        }
        return allPulls;
    }

    public List<Pull> backfillNumReviewersCommenters(String org, String repo) throws IllegalArgumentException, ServiceException {
        List<Pull> updatedPulls = Lists.newArrayList();

        List<Pull> allPulls = getAllPulls(org, repo, 500);
        if (allPulls == null || allPulls.isEmpty()) {
            return updatedPulls;
        }

        for (Pull pull : allPulls) {
            boolean updated = PullUtils.updateNumReviewersCommenters(pull);
            if (updated) {
                updatedPulls.add(pull);
            }
        }

        try {
            pullRepository.saveAll(updatedPulls);
        } catch (Exception e) {
            throw new ServiceException("Unable to save updated pulls for " + org + "/" + repo, e);
        }

        return updatedPulls;
    }

    public List<Pull> backfillTimeToFirstReviewAndFeedback(String org, String repo) throws IllegalArgumentException, ServiceException {
        List<Pull> updatedPulls = Lists.newArrayList();
        List<Pull> allPulls = getAllPulls(org, repo, 500);
        if (allPulls == null || allPulls.isEmpty()) {
            return updatedPulls;
        }

        for (Pull pull : allPulls) {
            boolean updated = PullUtils.updateTimeToFirstReviewAndFeedback(pull);
            if (updated) {
                updatedPulls.add(pull);
            }
        }

        try {
            pullRepository.saveAll(updatedPulls);
        } catch (Exception e) {
            throw new ServiceException("Unable to save updated pulls for " + org + "/" + repo, e);
        }

        return updatedPulls;
    }

    public Collection<Pull> getPullsByAuthor(final String org, final String repo, final String author, final PullState pullState, final PullDateType pullDateType, final String dateAfter) throws IllegalArgumentException, ServiceException {
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("repo is required");
        }
        if (author == null || author.isEmpty()) {
            throw new IllegalArgumentException("author is required");
        }
        if (pullState == null) {
            throw new IllegalArgumentException("state is required");
        }

        if (dateAfter != null && !dateAfter.isEmpty()) {
            try {
                LocalDate.parse(dateAfter);
            } catch (Exception e) {
                throw new IllegalArgumentException("dateAfter must be valid yyyy-MM-dd format if provided");
            }
        }

        Set<Pull> allPulls = Sets.newHashSet();

        if (pullDateType == null || dateAfter == null || dateAfter.isEmpty()) {
            try {
                List<PullState> pullStates = PullUtils.buildPullStateList(pullState);
                for (PullState state : pullStates) {
                    Page<Pull> page = pullRepository.findByOrgAndRepoAndAuthorAndStateOrderByCreatedAt(org, repo, author, state.name(), Pageable.ofSize(100));
                    while (page != null) {
                        List<Pull> pageList = page.toList();
                        if (pageList != null) {
                            allPulls.addAll(pageList);
                        }
                        if (page.hasNext()) {
                            page = pullRepository.findByOrgAndRepoAndAuthorAndStateOrderByCreatedAt(org, repo, author, state.name(), page.nextPageable());
                        } else {
                            page = null;
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServiceException("Unable to retrieve " + pullState.name() + " pull requests by " + author + " for " + org + "/" + repo, e);
            }
        } else {
            List<PullDateType> pullDateTypes = PullUtils.buildPullDateTypeList(pullDateType);
            List<PullState> pullStates = PullUtils.buildPullStateList(pullState);
            try {
                for (PullDateType dateType : pullDateTypes) {
                    for (PullState state : pullStates) {
                        Page<Pull> page = null;
                        switch (dateType) {
                            case UPDATED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndUpdatedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, Pageable.ofSize(100));
                            case CLOSED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndClosedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, Pageable.ofSize(100));
                            case CREATED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndCreatedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, Pageable.ofSize(100));
                            case MERGED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndMergedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, Pageable.ofSize(100));
                        }
                        while (page != null) {
                            List<Pull> pageList = page.toList();
                            if (pageList != null) {
                                allPulls.addAll(pageList);
                            }
                            if (page.hasNext()) {
                                switch (dateType) {
                                    case UPDATED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndUpdatedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, page.nextPageable());
                                    case CLOSED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndClosedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, page.nextPageable());
                                    case CREATED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndCreatedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, page.nextPageable());
                                    case MERGED -> page = pullRepository.findByOrgAndRepoAndAuthorAndStateAndMergedAtGreaterThanEqualOrderByCreatedAt(org, repo, author, state.name(), dateAfter, page.nextPageable());
                                }
                            } else {
                                page = null;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServiceException("Unable to retrieve pull requests by " + author + " for " + org + "/" + repo, e);
            }
        }

        return allPulls;
    }
}
