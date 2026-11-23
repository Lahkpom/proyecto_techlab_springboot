package com.proyecto.proyecto_techlab_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ProyectoTechlabSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectoTechlabSpringbootApplication.class, args);
    }

}
