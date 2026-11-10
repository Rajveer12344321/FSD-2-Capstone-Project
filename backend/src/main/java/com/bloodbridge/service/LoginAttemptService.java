package com.bloodbridge.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Brute-force protection: five failed logins for one email within ten minutes lock further attempts. */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Deque<Instant> q = failures.get(key);
        if (q == null) return;
        synchronized (q) {
            prune(q);
            if (q.size() >= MAX_FAILURES)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed attempts. Try again in a few minutes.");
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(Instant.now());
        }
    }

    public void clear(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> q) {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
    }
}
