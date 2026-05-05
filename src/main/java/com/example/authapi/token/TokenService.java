package com.example.authapi.token;

import com.example.authapi.auth.IssuedToken;
import com.example.authapi.config.AuthProperties;
import com.example.authapi.error.ApiException;
import com.example.authapi.user.UserAccount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public TokenService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    public IssuedToken issue(UserAccount user) {
        long now = Instant.now().getEpochSecond();
        long expiresIn = authProperties.getTokenTtlSeconds();
        long expiresAt = now + expiresIn;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("iat", now);
        payload.put("exp", expiresAt);

        String encodedHeader = base64UrlJson(header);
        String encodedPayload = base64UrlJson(payload);
        String signature = hmacSha256(encodedHeader + "." + encodedPayload, authProperties.getTokenSecret());

        return new IssuedToken(encodedHeader + "." + encodedPayload + "." + signature, expiresIn);
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_ERROR", "Cannot generate token");
        }
    }

    private static String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_ERROR", "Cannot generate token");
        }
    }
}
