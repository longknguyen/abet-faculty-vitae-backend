package edu.virginia.cs.abetvitae.parserworker;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects requests that do not carry the worker's private bearer token. */
@Component
final class ParserWorkerAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final byte[] expectedAuthorization;

    ParserWorkerAuthenticationFilter(ParserWorkerProperties properties) {
        expectedAuthorization = (BEARER_PREFIX + properties.token())
                .getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        byte[] presented = authorization == null
                ? new byte[0]
                : authorization.getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expectedAuthorization, presented)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorised\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
