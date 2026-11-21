package io.github.omarsherifsayed.tajweed.general.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds the security headers every answer carries.
 *
 * <p>The API has no login, so Spring Security is not used ({@code docs/plan/backend-plan.md} §4); these headers are
 * what a public, read-only JSON API needs: the browser must not guess content types, must not show the API inside a
 * frame, and must not send the page address on. Answers of {@code /v1/**} also forbid loading anything (they are
 * JSON, never a page); the Swagger page is left to work.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * Created by Spring.
     */
    public SecurityHeadersFilter() {
        // no state
    }

    /**
     * Adds the headers, then lets the request through.
     *
     * @param request  the request
     * @param response the response
     * @param chain    the rest of the filters and the controller
     * @throws ServletException if a later filter fails
     * @throws IOException      if a later filter fails to write
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (request.getRequestURI().startsWith("/v1/")) {
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }
        chain.doFilter(request, response);
    }
}
