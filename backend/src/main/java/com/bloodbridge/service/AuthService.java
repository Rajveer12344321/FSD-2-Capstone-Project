package com.bloodbridge.service;

import com.bloodbridge.domain.Role;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.UserRepository;
import com.bloodbridge.security.JwtService;
import com.bloodbridge.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PermissionService permissions;
    private final AuditService audit;
    private final LoginAttemptService attempts;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       PermissionService permissions, AuditService audit, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.permissions = permissions;
        this.audit = audit;
        this.attempts = attempts;
    }

    public AuthResponse register(RegisterRequest in) {
        if (in.role() != Role.DONOR && in.role() != Role.REQUESTER)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only donor and requester accounts can be self-registered");
        String email = in.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        User u = new User();
        u.setName(in.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(in.password()));
        u.setRole(in.role());
        u = users.save(u);
        audit.log(u, "REGISTER", "user#" + u.getId(), "role " + u.getRole());
        return response(u);
    }

    public AuthResponse login(LoginRequest in) {
        String email = in.email().trim().toLowerCase();
        attempts.checkAllowed(email);
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty() || !encoder.matches(in.password(), found.get().getPasswordHash())) {
            attempts.recordFailure(email);
            audit.logAs(email, "LOGIN_FAILED", "auth", "invalid credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User u = found.get();
        if (!u.isActive()) {
            audit.log(u, "LOGIN_BLOCKED", "auth", "account deactivated");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been deactivated");
        }
        attempts.clear(email);
        audit.log(u, "LOGIN", "auth", null);
        return response(u);
    }

    public UserDto describe(User u) {
        return UserDto.of(u, permissions.forRole(u.getRole()));
    }

    private AuthResponse response(User u) {
        return new AuthResponse(jwt.generate(u), describe(u));
    }
}
