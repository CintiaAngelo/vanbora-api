package com.vanbora.api.security.jwt;

import com.vanbora.api.security.AppUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lê o token Bearer, valida-o e popula o contexto de segurança a cada requisição. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AppUserDetailsService userDetailsService,
                                   TokenRevocationService tokenRevocationService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtService.isValid(token)
                && !tokenRevocationService.isRevoked(jwtService.extractTokenId(token))
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
