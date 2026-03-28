package com.nutricoach.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wati")
@Getter
@Setter
public class WatiProperties {

    private String apiEndpoint;
    private String apiToken;
}
