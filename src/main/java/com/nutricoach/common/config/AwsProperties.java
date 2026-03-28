package com.nutricoach.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
@Getter
@Setter
public class AwsProperties {

    private String region;
    private String accessKey;
    private String secretKey;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
    }
}
