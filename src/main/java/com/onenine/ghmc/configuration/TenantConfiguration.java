package com.onenine.ghmc.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("tenant")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantConfiguration {
    private String id;
    private String name;

    public Boolean isValid() {
        if (id == null || id.isEmpty() || id.trim().length() != 5) {
            return false;
        }
        return true;
    }
}
