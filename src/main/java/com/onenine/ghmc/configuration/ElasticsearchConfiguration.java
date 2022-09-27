package com.onenine.ghmc.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElasticsearchConfiguration {
    private boolean ssl = true;
    private String host = null;
    private Integer port = null;
    private String username = null;
    private String password = null;
    private boolean disableHostnameVerification = false;
    private boolean trustAllCertificates = false;
}
