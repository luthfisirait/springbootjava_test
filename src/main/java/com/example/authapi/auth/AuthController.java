package com.example.authapi.auth;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RegisterResponse register(@RequestParam("payload") String encryptedPayload) {
        return authService.register(encryptedPayload);
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public LoginResponse login(@RequestParam("payload") String encryptedPayload) {
        return authService.login(encryptedPayload);
    }
}
