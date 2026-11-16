package com.proyecto.proyecto_techlab_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ProyectoTechlabSpringbootApplication {

    /**
     * TODO
     *  - Agregar a las internal notes de las sale la hora del comentario y también quién lo hizo.
     * TODO
     *  - Reveer el shippingCost del flujo de sale, se lo está recibiendo por el front pero eso debería:
     *      - Estar seteado con un costo fijo en el back
     *      - Ser dinamico dándole un flujo y una tabla en la db donde pueda modificarse
     *      - Ver si los proveedores del servicio tienen alguna api que devuelva el valor actual y calcularlo en base a eso
     * TODO
     *   - Hacer un /model para user_security_tokens cuando implemente verificación de email o reseteo de clave
     *   - Ver también la lógica para que el usuario pueda modificar su correo
     * TODO
     *  - Poner el isAnonymous en un @PreAuthorize en los endpoints de login y register para evitar que accedan usuarios con sesión activa
     */


    public static void main(String[] args) {
        SpringApplication.run(ProyectoTechlabSpringbootApplication.class, args);
    }

}
