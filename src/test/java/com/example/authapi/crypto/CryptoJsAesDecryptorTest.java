package com.example.authapi.crypto;

import com.example.authapi.config.AuthProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoJsAesDecryptorTest {

    @Test
    void decryptsCryptoJsPassphrasePayload() throws Exception {
        AuthProperties properties = new AuthProperties();
        properties.setCryptoSecret("test-secret");

        CryptoJsAesDecryptor decryptor = new CryptoJsAesDecryptor(properties);
        String json = "{\"email\":\"user@example.com\",\"password\":\"password123\"}";
        String encryptedPayload = encryptLikeCryptoJs(json, "test-secret", "12345678".getBytes(StandardCharsets.US_ASCII));

        assertThat(decryptor.decrypt(encryptedPayload)).isEqualTo(json);
    }

    private static String encryptLikeCryptoJs(String value, String passphrase, byte[] salt) throws Exception {
        KeyAndIv keyAndIv = deriveKeyAndIv(passphrase, salt);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyAndIv.key(), "AES"),
                new IvParameterSpec(keyAndIv.iv())
        );

        byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] prefix = "Salted__".getBytes(StandardCharsets.US_ASCII);
        byte[] encrypted = new byte[prefix.length + salt.length + cipherText.length];
        System.arraycopy(prefix, 0, encrypted, 0, prefix.length);
        System.arraycopy(salt, 0, encrypted, prefix.length, salt.length);
        System.arraycopy(cipherText, 0, encrypted, prefix.length + salt.length, cipherText.length);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static KeyAndIv deriveKeyAndIv(String passphrase, byte[] salt) throws Exception {
        byte[] password = passphrase.getBytes(StandardCharsets.UTF_8);
        byte[] keyAndIv = new byte[48];
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
                Arrays.copyOfRange(keyAndIv, 0, 32),
                Arrays.copyOfRange(keyAndIv, 32, 48)
        );
    }

    private record KeyAndIv(byte[] key, byte[] iv) {
    }
}
