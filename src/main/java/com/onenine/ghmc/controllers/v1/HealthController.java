package com.onenine.ghmc.controllers.v1;

import com.onenine.ghmc.configuration.ApplicationConfiguration;
import com.onenine.ghmc.models.Health;
import com.onenine.ghmc.services.GhClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("api/v1/health")
public class HealthController {
    private final ApplicationConfiguration applicationConfiguration;
    private final GhClientService ghClientService;

    @Autowired
    public HealthController(ApplicationConfiguration applicationConfiguration, GhClientService ghClientService) {
        this.applicationConfiguration = applicationConfiguration;
        this.ghClientService = ghClientService;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Health getHealth() {
        return checkHealth();
    }

    @GetMapping(value = "/ready")
    public ResponseEntity<Boolean> getReady() {
        boolean ready = checkHealth().isReady();
        return new ResponseEntity<Boolean>(ready, ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    @GetMapping(value = "/live")
    public ResponseEntity<Boolean> getLive() {
        boolean live = checkHealth().isLive();
        return new ResponseEntity<Boolean>(live, live ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    private Health checkHealth() {
        return new Health(true, applicationConfiguration != null && applicationConfiguration.isValid() && ghClientService.getClient() != null);
    }
}
