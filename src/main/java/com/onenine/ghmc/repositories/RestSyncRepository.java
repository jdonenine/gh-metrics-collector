package com.onenine.ghmc.repositories;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.models.Sync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RestSyncRepository implements SyncRepository {
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public RestSyncRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public Optional<Sync> getSyncByOrgAndRepoAndPullStateAndPullDateTypeOrderByTimestampDesc(String org, String repo, PullState pullState, PullDateType pullDateType) {
        return Optional.empty();
    }

    @Override
    public Sync save(Sync sync) {
        return null;
    }
}
