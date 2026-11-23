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
     *  - Hacer que el internal notes no pueda modificarse lo que ya se escribió, que solo puedan agregarse más notas
     *  - Al guardar cada nota, siempre debe empezar con '[userName dd/mm/yyyy 00:00] nota' donde
     *      - userName es el nombre del usuario que generó la nota
     *      - fecha y hora en la que se hizo
     *      - nota es la nota escrita por el usuario para guardar
     *  - Las notas deben guardarse de modo que las nuevas notas vayan quedando arriba de las viejas notas
     *  - Después ver en el front que cuando se habre una sale, en la parte de las internalNotes, que abajo del campo para agregar nuevas notas se imprima el estado actual (Donde estará ordenado cronológicamente)
     *  - También en todo el flujo del sistema en donde el sistema ingresa notas internas, debe quedar claro, con el mismo formato, que fue el sistéma y que sector del sistema ingresó esa nota
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
