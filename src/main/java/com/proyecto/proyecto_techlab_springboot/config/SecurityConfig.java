package com.proyecto.proyecto_techlab_springboot.config;

import com.proyecto.proyecto_techlab_springboot.auth.security.CustomAccessDeniedHandler;
import com.proyecto.proyecto_techlab_springboot.auth.security.CustomAuthenticationEntryPoint;
import com.proyecto.proyecto_techlab_springboot.auth.security.JwtAuthenticationFilter;
import com.proyecto.proyecto_techlab_springboot.auth.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            CustomAuthenticationEntryPoint entryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/change-expired-password"
                        ).anonymous()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                                .requestMatchers("/api/dev/**").permitAll()
                                .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/variants/**",
                                "/api/colors/**",
                                "/api/shipping/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/cart/sync",
                                "/api/sales/checkout",
                                "/api/webhooks/**",
                                "/api/payments/mercadopago/webhook",
                                "/api/returns/guest/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/sales/track/**",
                                "/api/returns/track/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}