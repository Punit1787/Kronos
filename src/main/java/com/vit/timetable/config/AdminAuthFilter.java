package com.vit.timetable.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

/**
 * AdminAuthFilter — Protects all mutating API endpoints (POST, PUT, DELETE)
 * with a simple password. GET requests are public (anyone can view).
 *
 * Set the admin password via the ADMIN_PASSWORD environment variable.
 * Default: "admin123" (change this in production!)
 *
 * The frontend sends the password as a Bearer token in the Authorization header.
 */
@Component
@Order(1)
public class AdminAuthFilter implements Filter {

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String method = req.getMethod();
        String path = req.getRequestURI();

        // Only protect mutating operations on /api/ endpoints
        // GET requests are public (viewing timetable, listing subjects, etc.)
        boolean isMutating = method.equals("POST") || method.equals("PUT") || method.equals("DELETE");
        boolean isApiPath = path.startsWith("/api/");
        // Don't protect the login check endpoint
        boolean isAuthEndpoint = path.equals("/api/auth/check");

        if (isMutating && isApiPath && !isAuthEndpoint) {
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Admin login required\"}");
                return;
            }

            String token = authHeader.substring(7);
            // Token is base64-encoded password
            try {
                String decoded = new String(Base64.getDecoder().decode(token));
                if (!decoded.equals(adminPassword)) {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Invalid admin password\"}");
                    return;
                }
            } catch (Exception e) {
                res.setStatus(403);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
