package com.inventario.sucursales.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// Configuración de seguridad Spring Security, filtros JWT, CORS y políticas de acceso RBAC
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF para API REST sin estado
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Gestión de sesión sin estado
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Manejo de excepciones de autenticación y autorización (401 y 403 con JSON)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"No autorizado: Se requiere token JWT valido para acceder a este recurso.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Acceso denegado: No posee los permisos requeridos para esta operacion.\"}");
                        }))
                // Cabeceras HTTP de seguridad
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net data:; img-src 'self' data: https:; connect-src 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                // Reglas de autorización por endpoints y roles
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (Login, Frontend estático, Health check)
                        .requestMatchers(
                                "/api/auth/**",
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/actuator/health")
                        .permitAll()

                        // Operaciones exclusivas del Administrador (ROLE_ADMIN)
                        .requestMatchers("/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/requests/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/requests/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/branches/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/branches/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/branches/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Operaciones accesibles por usuarios autenticados (ADMIN y GERENTE)
                        .requestMatchers(HttpMethod.GET, "/api/branches/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/requests/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/requests/**").hasAnyRole("ADMIN", "GERENTE")

                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
