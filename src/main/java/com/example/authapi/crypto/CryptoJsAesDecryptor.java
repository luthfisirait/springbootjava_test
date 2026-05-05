package com.example.authapi.crypto;

import com.example.authapi.config.AuthProperties;
import com.example.authapi.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Component
public class CryptoJsAesDecryptor {

    private static final byte[] SALTED_PREFIX = "Salted__".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_LENGTH = 8;
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private final AuthProperties authProperties;

    public CryptoJsAesDecryptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String decrypt(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_PAYLOAD", "Form field 'payload' is required");
        }

        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPayload.trim().replace(' ', '+'));
            if (encryptedBytes.length <= SALTED_PREFIX.length + SALT_LENGTH) {
                throw invalidEncryptedPayload();
            }

            byte[] prefix = Arrays.copyOfRange(encryptedBytes, 0, SALTED_PREFIX.length);
            if (!Arrays.equals(prefix, SALTED_PREFIX)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "UNSUPPORTED_PAYLOAD_FORMAT",
                        "Payload must be generated with CryptoJS.AES.encrypt(value, passphrase).toString()"
                );
            }

            byte[] salt = Arrays.copyOfRange(encryptedBytes, SALTED_PREFIX.length, SALTED_PREFIX.length + SALT_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(encryptedBytes, SALTED_PREFIX.length + SALT_LENGTH, encryptedBytes.length);
            KeyAndIv keyAndIv = deriveKeyAndIv(authProperties.getCryptoSecret(), salt);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyAndIv.key(), "AES"),
                    new IvParameterSpec(keyAndIv.iv())
            );
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidEncryptedPayload();
        }
    }

    private static KeyAndIv deriveKeyAndIv(String passphrase, byte[] salt) throws NoSuchAlgorithmException {
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

        byte[] key = Arrays.copyOfRange(keyAndIv, 0, KEY_LENGTH);
        byte[] iv = Arrays.copyOfRange(keyAndIv, KEY_LENGTH, KEY_LENGTH + IV_LENGTH);
        return new KeyAndIv(key, iv);
    }

    private static ApiException invalidEncryptedPayload() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ENCRYPTED_PAYLOAD",
                "Payload cannot be decrypted"
        );
    }

    private record KeyAndIv(byte[] key, byte[] iv) {
    }
}
