package com.nutricoach.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService coachUserDetailsService;
    private final UserDetailsService clientUserDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Qualifier("coachUserDetailsService") UserDetailsService coachUserDetailsService,
            @Qualifier("clientUserDetailsService") UserDetailsService clientUserDetailsService) {
        this.jwtService = jwtService;
        this.coachUserDetailsService = coachUserDetailsService;
        this.clientUserDetailsService = clientUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String phone = jwtService.extractPhone(jwt);

            if (StringUtils.hasText(phone) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String role = jwtService.extractRole(jwt);
                boolean isClient = "ROLE_CLIENT".equals(role);

                UserDetailsService svc = isClient ? clientUserDetailsService : coachUserDetailsService;
                UserDetails userDetails = svc.loadUserByUsername(phone);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // Attach JWT claims as details so SecurityUtils can read them without a DB roundtrip
                    Map<String, String> details = new HashMap<>();
                    details.put("role", role);
                    if (isClient) {
                        details.put("clientId", jwtService.extractClientId(jwt).toString());
                        details.put("coachId", jwtService.extractCoachId(jwt).toString());
                    } else {
                        details.put("coachId", jwtService.extractCoachId(jwt).toString());
                    }
                    authToken.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
