package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "DIRECTEUR")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "DIRECTEUR")
                        .requestMatchers("/api/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/app-notifications/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/equipements").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/equipements/affecter",
                                "/api/equipements/affecter-piece-directe",
                                "/api/equipements/affecter-pack").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pannes").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.POST, "/api/pannes/publier").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reparations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/prets").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET, "/api/reparations/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reparations/*/cloturer")
                                .hasAnyRole("ADMIN", "TECHNICIEN")
                        .requestMatchers(HttpMethod.PUT, "/api/reparations/*/executer")
                                .hasRole("TECHNICIEN")
                        .requestMatchers(HttpMethod.PUT, "/api/pannes/*/affecter/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pannes/*/publier").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pannes/*/claim").hasRole("TECHNICIEN")
                        .requestMatchers(HttpMethod.PUT, "/api/pannes/*/statut/*").hasAnyRole("ADMIN", "TECHNICIEN")
                        .requestMatchers("/api/stock/**", "/api/reparations/**").hasAnyRole("ADMIN", "TECHNICIEN")
                        .requestMatchers(HttpMethod.PUT, "/api/prets/*/valider", "/api/prets/*/refuser",
                                "/api/prets/*/cloturer", "/api/prets/*/prolonger").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/fournisseurs/**", "/api/services/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
