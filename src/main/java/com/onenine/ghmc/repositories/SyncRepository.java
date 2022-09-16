package com.onenine.ghmc.repositories;

import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.models.Sync;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface SyncRepository extends ElasticsearchRepository<Sync, String> {
    Optional<Sync> getSyncByOrgAndRepoAndPullStateAndPullDateTypeOrderByTimestampDesc(String org, String repo, PullState pullState, PullDateType pullDateType);
}
