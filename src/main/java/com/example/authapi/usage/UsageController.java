package com.example.authapi.usage;

import com.example.authapi.config.AuthProperties;
import com.example.authapi.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UsageController {

    private static final byte[] SALTED_PREFIX = "Salted__".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_LENGTH = 8;
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;
    private static final String SAMPLE_EMAIL = "user@example.com";
    private static final String SAMPLE_PASSWORD = "password123";

    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public UsageController(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @GetMapping(value = "/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public UsageResponse usage() {
        String sampleJson = "{\"email\":\"" + SAMPLE_EMAIL + "\",\"password\":\"" + SAMPLE_PASSWORD + "\"}";
        String registerPayload = encryptForCryptoJs(sampleJson);
        String loginPayload = encryptForCryptoJs(sampleJson);

        return new UsageResponse(
                true,
                "Auth API usage",
                "application/x-www-form-urlencoded",
                "Send one form field named 'payload'. Its value must be CryptoJS AES encrypted JSON credentials.",
                new SampleCredentials(SAMPLE_EMAIL, SAMPLE_PASSWORD, sampleJson),
                List.of(
                        new EndpointUsage(
                                "register",
                                "POST",
                                "/api/auth/register",
                                "Register a new user",
                                "Content-Type: application/x-www-form-urlencoded",
                                "payload=" + UriUtils.encodeQueryParam(registerPayload, StandardCharsets.UTF_8),
                                registerPayload,
                                "Run register first before testing login with the same sample user."
                        ),
                        new EndpointUsage(
                                "login",
                                "POST",
                                "/api/auth/login",
                                "Login with registered user",
                                "Content-Type: application/x-www-form-urlencoded",
                                "payload=" + UriUtils.encodeQueryParam(loginPayload, StandardCharsets.UTF_8),
                                loginPayload,
                                "Use this after the sample user has been registered."
                        )
                ),
                """
                        const payload = CryptoJS.AES
                          .encrypt(JSON.stringify({ email: "user@example.com", password: "password123" }), CRYPTO_SECRET)
                          .toString();

                        const body = new URLSearchParams();
                        body.set("payload", payload);
                        """
        );
    }

    private String encryptForCryptoJs(String value) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            KeyAndIv keyAndIv = deriveKeyAndIv(authProperties.getCryptoSecret(), salt);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyAndIv.key(), "AES"),
                    new IvParameterSpec(keyAndIv.iv())
            );

            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = new byte[SALTED_PREFIX.length + salt.length + cipherText.length];
            System.arraycopy(SALTED_PREFIX, 0, encrypted, 0, SALTED_PREFIX.length);
            System.arraycopy(salt, 0, encrypted, SALTED_PREFIX.length, salt.length);
            System.arraycopy(cipherText, 0, encrypted, SALTED_PREFIX.length + salt.length, cipherText.length);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "USAGE_GENERATION_ERROR", "Cannot generate usage samples");
        }
    }

    private static KeyAndIv deriveKeyAndIv(String passphrase, byte[] salt) throws Exception {
        byte[] password = passphrase.getBytes(StandardCharsets.UTF_8);
        byte[] keyAndIv = new byte[KEY_LENGTH + IV_LENGTH];
        byte[] previous = new byte[0];
        int copied = 0;

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        while (copied < keyAndIv.length) {
            md5.reset();
            md5.update(previous);
            md5.update(password);
            md5.update(salt);
            previous = md5.digest();

            int bytesToCopy = Math.min(previous.length, keyAndIv.length - copied);
            System.arraycopy(previous, 0, keyAndIv, copied, bytesToCopy);
            copied += bytesToCopy;
        }

        return new KeyAndIv(
                Arrays.copyOfRange(keyAndIv, 0, KEY_LENGTH),
                Arrays.copyOfRange(keyAndIv, KEY_LENGTH, KEY_LENGTH + IV_LENGTH)
        );
    }

    private record KeyAndIv(byte[] key, byte[] iv) {
    }

    public record UsageResponse(
            boolean success,
            String title,
            String contentType,
            String requestRule,
            SampleCredentials sampleCredentials,
            List<EndpointUsage> endpoints,
            String cryptoJsExample
    ) {
    }

    public record SampleCredentials(
            String email,
            String password,
            String jsonBeforeEncryption
    ) {
    }

    public record EndpointUsage(
            String name,
            String method,
            String path,
            String description,
            String header,
            String textBodyForTesting,
            String encryptedPayloadOnly,
            String note
    ) {
    }
}
