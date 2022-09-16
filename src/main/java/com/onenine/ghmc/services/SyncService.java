package com.onenine.ghmc.services;

import com.onenine.ghmc.exceptions.ServiceException;
import com.onenine.ghmc.models.PullDateType;
import com.onenine.ghmc.models.PullState;
import com.onenine.ghmc.models.Sync;
import com.onenine.ghmc.repositories.SyncRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SyncService {
    private final SyncRepository syncRepository;

    @Autowired
    public SyncService(SyncRepository syncRepository) {
        this.syncRepository = syncRepository;
    }

    public Sync saveSync(Sync sync) throws IllegalArgumentException, ServiceException {
        if (sync == null) {
            throw new IllegalArgumentException("sync is required");
        }
        try {
            return syncRepository.save(sync);
        } catch (Exception e) {
            throw new ServiceException("Unable to save sync", e);
        }
    }

    public Sync getLatestSync(String org, String repo, PullState pullState, PullDateType pullDateType) throws IllegalArgumentException, ServiceException {
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("repo is required");
        }
        if (pullState == null) {
            throw new IllegalArgumentException("pullState is required");
        }
        if (pullDateType == null) {
            throw new IllegalArgumentException("pullDateType is required");
        }
        try {
            return syncRepository.getSyncByOrgAndRepoAndPullStateAndPullDateTypeOrderByTimestampDesc(org, repo, pullState, pullDateType).orElse(null);
        } catch (Exception e) {
            throw new ServiceException("Unable to retrieve sync", e);
        }
    }
}
