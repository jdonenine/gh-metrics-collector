package com.onenine.ghmc.repositories;

import com.onenine.ghmc.models.Pull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface PullRepository extends ElasticsearchRepository<Pull, String> {
    Optional<Pull> findByOrgAndRepoAndNumber(String org, String repo, Integer number);

    Page<Pull> findByOrgAndRepoOrderByCreatedAt(String org, String repo, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateOrderByCreatedAt(String org, String repo, String author, String state, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndCreatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndUpdatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndClosedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndMergedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);
}
