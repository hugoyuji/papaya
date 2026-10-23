package br.com.papaya.service;

public class LoginAttemptLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public LoginAttemptLimitException(long retryAfterSeconds) {
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
