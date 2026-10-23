package br.com.papaya.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class AuthenticatedSessionLifetimeFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_AT_ATTRIBUTE = "papaya.authenticatedAt";

    private final Duration maxAuthenticatedLifetime;

    public AuthenticatedSessionLifetimeFilter(Duration maxAuthenticatedLifetime) {
        if (maxAuthenticatedLifetime.isNegative() || maxAuthenticatedLifetime.isZero()) {
            throw new IllegalArgumentException("The authenticated session lifetime must be positive");
        }
        this.maxAuthenticatedLifetime = maxAuthenticatedLifetime;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);
        if (authentication == null || !authentication.isAuthenticated() || session == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Object authenticatedAtValue = session.getAttribute(AUTHENTICATED_AT_ATTRIBUTE);
        if (!(authenticatedAtValue instanceof Instant authenticatedAt)
                || !Instant.now().isBefore(authenticatedAt.plus(maxAuthenticatedLifetime))) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
