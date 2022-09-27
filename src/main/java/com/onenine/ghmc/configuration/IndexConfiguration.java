package com.onenine.ghmc.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexConfiguration {
    private Integer replicas;
    private Integer shards;
}
