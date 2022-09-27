package com.onenine.ghmc.repositories;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.common.collect.Lists;
import com.onenine.ghmc.configuration.TenantConfiguration;
import com.onenine.ghmc.exceptions.RepositoryException;
import com.onenine.ghmc.models.Pull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RestPullRepository implements PullRepository {
    private final TenantConfiguration tenantConfiguration;
    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;

    @Autowired
    public RestPullRepository(TenantConfiguration tenantConfiguration, ElasticsearchClient elasticsearchClient) {
        this.tenantConfiguration = tenantConfiguration;
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = "pull-" + tenantConfiguration.getId();
    }

    @Override
    public Optional<Hit<Pull>> findByOrgAndRepoAndNumber(String org, String repo, Integer number) throws IOException {
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .match(ma -> ma
                                                .field("org")
                                                .query(org)
                                        )
                                )
                                .must(m -> m
                                        .match(ma -> ma
                                                .field("repo")
                                                .query(repo)
                                        )
                                )
                                .must(m -> m
                                        .match(ma -> ma
                                                .field("number")
                                                .query(number)
                                        )
                                )
                        )
                )
        );
        SearchResponse<Pull> searchResponse = elasticsearchClient.search(searchRequest, Pull.class);
        return searchResponse != null && searchResponse.hits() != null && searchResponse.hits().hits() != null && !searchResponse.hits().hits().isEmpty() ? searchResponse.hits().hits().stream().findFirst() : Optional.empty();
    }

    @Override
    public Page<Pull> findByOrgAndRepoOrderByCreatedAt(String org, String repo, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Pull> findByOrgAndRepoAndAuthorAndStateOrderByCreatedAt(String org, String repo, String author, String state, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Pull> findByOrgAndRepoAndAuthorAndStateAndCreatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Pull> findByOrgAndRepoAndAuthorAndStateAndUpdatedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Pull> findByOrgAndRepoAndAuthorAndStateAndClosedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Pull> findByOrgAndRepoAndAuthorAndStateAndMergedAtGreaterThanEqualOrderByCreatedAt(String org, String repo, String author, String state, String dateAfter, Pageable pageable) {
        return null;
    }

    @Override
    public Pull save(Pull pull) {
        return null;
    }

    @Override
    public Iterable<Pull> saveAll(List<Pull> pulls) throws IOException, RepositoryException {
        BulkResponse bulkResponse = elasticsearchClient.bulk(b -> b
                .index(indexName)
                .operations(pulls.stream().map(pull -> BulkOperation.of(bo -> bo
                        .index(i -> i
                                .index(indexName)
                                .document(pull)
                        )
                )).collect(Collectors.toList()))
        );
        if (bulkResponse.errors()) {
            throw new RepositoryException("An error occurred executing bulk operation");
        }
        return pulls;
    }

    @Override
    public Page<Pull> findAll(Pageable nextPageable) {
        return null;
    }
}
