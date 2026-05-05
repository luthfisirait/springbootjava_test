package com.example.authapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String cryptoSecret;
    private String tokenSecret;
    private long tokenTtlSeconds = 3600;

    public String getCryptoSecret() {
        return cryptoSecret;
    }

    public void setCryptoSecret(String cryptoSecret) {
        this.cryptoSecret = cryptoSecret;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }
}
