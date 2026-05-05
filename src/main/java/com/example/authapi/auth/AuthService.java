package com.example.authapi.auth;

import com.example.authapi.crypto.CryptoJsAesDecryptor;
import com.example.authapi.error.ApiException;
import com.example.authapi.token.TokenService;
import com.example.authapi.user.UserAccount;
import com.example.authapi.user.UserAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoJsAesDecryptor decryptor;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            CryptoJsAesDecryptor decryptor,
            ObjectMapper objectMapper,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.decryptor = decryptor;
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
    }

    @Transactional
    public RegisterResponse register(String encryptedPayload) {
        AuthCredentials credentials = readCredentials(encryptedPayload);
        String email = normalizeEmail(credentials.getEmail());
        validateCredentials(email, credentials.getPassword());

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email already registered");
        }

        UserAccount user = userRepository.save(new UserAccount(
                email,
                passwordEncoder.encode(credentials.getPassword())
        ));

        return new RegisterResponse(true, user.getId(), user.getEmail(), "Registration successful");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String encryptedPayload) {
        AuthCredentials credentials = readCredentials(encryptedPayload);
        String email = normalizeEmail(credentials.getEmail());
        validateCredentials(email, credentials.getPassword());

        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        IssuedToken issuedToken = tokenService.issue(user);
        return new LoginResponse(
                true,
                user.getId(),
                user.getEmail(),
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresIn()
        );
    }

    private AuthCredentials readCredentials(String encryptedPayload) {
        String json = decryptor.decrypt(encryptedPayload);

        try {
            return objectMapper.readValue(json, AuthCredentials.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD", "Decrypted payload must be valid JSON");
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateCredentials(String email, String password) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "Email format is invalid");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PASSWORD",
                    "Password must contain at least " + MIN_PASSWORD_LENGTH + " characters"
            );
        }
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }
}
