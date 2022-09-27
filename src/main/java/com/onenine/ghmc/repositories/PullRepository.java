package com.onenine.ghmc.repositories;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.onenine.ghmc.exceptions.RepositoryException;
import com.onenine.ghmc.models.Pull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface PullRepository {
    Optional<Hit<Pull>> findByOrgAndRepoAndNumber(String org, String repo, Integer number) throws IOException;

    Page<Pull> findByOrgAndRepoOrderByCreatedAt(String org, String repo, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateOrderByCreatedAt(String org, String repo, String author, String state, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndCreatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndUpdatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndClosedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Page<Pull> findByOrgAndRepoAndAuthorAndStateAndMergedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable);

    Pull save(Pull pull);

    Iterable<Pull> saveAll(List<Pull> pulls) throws IOException, RepositoryException;

    Page<Pull> findAll(Pageable nextPageable);
}
