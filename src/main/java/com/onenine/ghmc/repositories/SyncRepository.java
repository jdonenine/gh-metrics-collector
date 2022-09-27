package com.onenine.ghmc.repositories;

import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.models.Sync;

import java.util.Optional;

public interface SyncRepository {
    Optional<Sync> getSyncByOrgAndRepoAndPullStateAndPullDateTypeOrderByTimestampDesc(String org, String repo, PullState pullState, PullDateType pullDateType);

    Sync save(Sync sync);
}
