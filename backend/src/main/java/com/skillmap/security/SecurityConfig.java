package com.skillmap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

// ─────────────────────────────────────────
//  SECURITY CONFIG
// ─────────────────────────────────────────
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${FRONTEND_URL_STAGING:https://skillmap-staging.web.app}")
    private String stagingUrl;

    @Value("${FRONTEND_URL_PROD:https://skillmap.web.app}")
    private String prodUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            RateLimitFilter rateLimitFilter) throws Exception {
        return http
            // Pas de CSRF (API REST stateless)
            .csrf(csrf -> csrf.disable())

            // CORS — seulement les domaines Firebase autorisés
            .cors(cors -> cors.configurationSource(corsConfigSource()))

            // HTTPS obligatoire en prod (désactivé en local)
            // .requiresChannel(ch -> ch.anyRequest().requiresSecure())

            // Pas de session serveur (stateless JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Autorisations
            // NB : on force AntPathRequestMatcher (antMatcher) au lieu du
            // MvcRequestMatcher par défaut. Ce dernier repasse par le
            // HandlerMappingIntrospector et matche de façon incohérente les
            // endpoints ayant des sous-chemins (ex: /api/skills + /api/skills/compare),
            // ce qui renvoyait des 401 sur /api/skills et /api/cities.
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (lecture du dashboard) — toutes les lectures GET /api/**
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/**")).permitAll()
                // Health check GCP Cloud Run
                .requestMatchers(antMatcher("/actuator/health")).permitAll()
                .requestMatchers(antMatcher("/actuator/info")).permitAll()
                // Tout le reste nécessite un JWT
                .anyRequest().authenticated()
            )

            // JWT via OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults()))

            // Headers de sécurité
            .headers(headers -> headers
                .frameOptions(fo -> fo.deny())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'"))
            )

            // Rate Limiting avant la chaîne de filtres
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:4200", // Angular dev
            stagingUrl,
            prodUrl
        ));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

// ─────────────────────────────────────────
//  RATE LIMIT FILTER (Redis)
// ─────────────────────────────────────────
@Component
@Slf4j
@RequiredArgsConstructor
class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Integer> redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws IOException, jakarta.servlet.ServletException {

        // Passe le health check sans rate limiting
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;

        try {
            Integer count = redisTemplate.opsForValue().get(key);

            if (count != null && count >= MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit dépassé pour IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"retryAfter\":60}");
                return;
            }

            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);

        } catch (Exception e) {
            // Si Redis est down, on laisse passer (fail-open)
            log.warn("Redis indisponible pour rate limiting — requête autorisée");
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
