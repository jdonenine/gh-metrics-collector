package com.onenine.ghmc.controllers.v1;

import com.onenine.ghmc.services.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/sync")
public class SyncController {
    private final SyncService syncService;

    @Autowired
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }
}
