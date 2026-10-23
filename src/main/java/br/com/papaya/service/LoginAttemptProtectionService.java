package br.com.papaya.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class LoginAttemptProtectionService {

    private final JdbcTemplate jdbcTemplate;
    private final int accountMaxFailures;
    private final int ipMaxFailures;
    private final Duration failureWindow;

    public LoginAttemptProtectionService(
            JdbcTemplate jdbcTemplate,
            @Value("${papaya.security.login.account-max-failures:5}") int accountMaxFailures,
            @Value("${papaya.security.login.ip-max-failures:20}") int ipMaxFailures,
            @Value("${papaya.security.login.failure-window:15m}") Duration failureWindow
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountMaxFailures = accountMaxFailures;
        this.ipMaxFailures = ipMaxFailures;
        this.failureWindow = failureWindow;
    }

    public void checkAllowed(String email, String ipAddress) {
        checkLimit("ACCOUNT", hash(normalizeEmail(email)), accountMaxFailures);
        checkLimit("IP", hash(ipAddress), ipMaxFailures);
    }

    public void recordFailure(String email, String ipAddress) {
        recordFailureForKey("ACCOUNT", hash(normalizeEmail(email)));
        recordFailureForKey("IP", hash(ipAddress));
    }

    public void clearAccountFailures(String email) {
        jdbcTemplate.update("DELETE FROM auth_login_failures WHERE scope = 'ACCOUNT' AND identifier_hash = ?",
                hash(normalizeEmail(email)));
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void removeExpiredFailureRecords() {
        jdbcTemplate.update("DELETE FROM auth_login_failures WHERE updated_at < ?",
                Timestamp.from(Instant.now().minus(failureWindow.multipliedBy(2))));
    }

    private void checkLimit(String scope, String identifierHash, int maxFailures) {
        Optional<FailureWindow> window = jdbcTemplate.query(
                "SELECT failure_count, window_started_at FROM auth_login_failures "
                        + "WHERE scope = ? AND identifier_hash = ?",
                resultSet -> resultSet.next()
                        ? Optional.of(new FailureWindow(resultSet.getInt("failure_count"),
                        resultSet.getTimestamp("window_started_at").toInstant()))
                        : Optional.empty(),
                scope, identifierHash);

        window.filter(value -> value.failureCount() >= maxFailures)
                .filter(value -> value.startedAt().plus(failureWindow).isAfter(Instant.now()))
                .ifPresent(value -> {
                    long retryAfter = Duration.between(Instant.now(), value.startedAt().plus(failureWindow)).toSeconds();
                    throw new LoginAttemptLimitException(retryAfter);
                });
    }

    private void recordFailureForKey(String scope, String identifierHash) {
        jdbcTemplate.update("""
                INSERT INTO auth_login_failures (scope, identifier_hash, failure_count, window_started_at, updated_at)
                VALUES (?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (scope, identifier_hash) DO UPDATE SET
                    failure_count = CASE
                        WHEN auth_login_failures.window_started_at <= CURRENT_TIMESTAMP - (? * INTERVAL '1 second')
                            THEN 1
                        ELSE auth_login_failures.failure_count + 1
                    END,
                    window_started_at = CASE
                        WHEN auth_login_failures.window_started_at <= CURRENT_TIMESTAMP - (? * INTERVAL '1 second')
                            THEN CURRENT_TIMESTAMP
                        ELSE auth_login_failures.window_started_at
                    END,
                    updated_at = CURRENT_TIMESTAMP
                """, scope, identifierHash, failureWindow.toSeconds(), failureWindow.toSeconds());
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record FailureWindow(int failureCount, Instant startedAt) {
    }
}
