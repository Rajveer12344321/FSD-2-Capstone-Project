package com.bloodbridge.web;

import com.bloodbridge.domain.User;
import com.bloodbridge.repo.UserRepository;
import com.bloodbridge.service.AuthService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest in) {
        return auth.register(in);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest in) {
        return auth.login(in);
    }

    /** Fresh role and permissions for the signed-in user (the UI uses this to show only permitted actions). */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        User fresh = users.findById(user.getId()).orElse(user);
        return auth.describe(fresh);
    }
}
