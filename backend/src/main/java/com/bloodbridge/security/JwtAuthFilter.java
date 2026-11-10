package com.bloodbridge.security;

import com.bloodbridge.repo.UserRepository;
import com.bloodbridge.service.PermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Validates the JWT and loads the user's role and permissions as authorities on every request. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final PermissionService permissions;

    public JwtAuthFilter(JwtService jwt, UserRepository users, PermissionService permissions) {
        this.jwt = jwt;
        this.users = users;
        this.permissions = permissions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwt.validate(header.substring(7)).flatMap(users::findByEmail).ifPresent(user -> {
                if (user.isActive()) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    permissions.forRole(user.getRole())
                            .forEach(p -> authorities.add(new SimpleGrantedAuthority(p.authority())));
                    SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
                }
            });
        }
        chain.doFilter(request, response);
    }
}
