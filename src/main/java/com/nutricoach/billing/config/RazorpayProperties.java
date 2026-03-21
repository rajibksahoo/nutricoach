package com.nutricoach.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.razorpay")
@Getter
@Setter
public class RazorpayProperties {

    private String keyId;
    private String keySecret;
    private String webhookSecret;
    private boolean devMode;
    private Map<String, String> planIds;

    public String planIdFor(String tier) {
        return planIds != null ? planIds.getOrDefault(tier.toLowerCase(), "") : "";
    }
}
