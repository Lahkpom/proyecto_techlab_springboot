package com.proyecto.proyecto_techlab_springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@Profile("dev")
@EnableAsync
public class DevAsyncConfiguration {
}
