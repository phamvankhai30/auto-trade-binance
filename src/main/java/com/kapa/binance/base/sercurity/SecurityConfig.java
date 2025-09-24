package com.kapa.binance.base.sercurity;

import com.kapa.binance.config.EnvConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final EnvConfig envConfig;
    private final CustomAuthFilter customAuthFilter;

    @Primary
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!envConfig.isEnabledSecurity()) {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }

        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(envConfig.getWhitelist().toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(customAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
