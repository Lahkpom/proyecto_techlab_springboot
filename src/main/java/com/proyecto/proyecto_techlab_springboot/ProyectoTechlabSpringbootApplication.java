package com.proyecto.proyecto_techlab_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProyectoTechlabSpringbootApplication {

    /**
     * TODO
     *  - Auth (Controller/Service). El caso del Repository no se si hacerle uno específico o usar el mismo de Users
     *      - auth/register
     *          - Hay que mover la lógica de createUser y CreateWholesaler para esta ruta.
     *          - Ver que también checkee que el email no esté duplicado
     *      - auth/login
     *      - auth/logout
     *      - auth/refresh-token
     * TODO
     *  - Cart. Ver lo necesario para que esto funcione a fin de entregarle al front un cart compuesto de cartItems
     * TODO
     *  - Sale. Ver cómo agregar la parte de sales y lo que es los saleItems
     * TODO
     *  - Agregar también las tablas de logs y session y armar la lógica para que se generen esos registros
     * TODO
     *  - Ver el flujo de los productos.
     *      - Cuando un producto se inactiva o elimina hace lo mismo en cascada con sus variantes
     *      - Tratar de hacer que solo se cambie el estado al producto y termine la transacción, y que la cascada en lasvariantes se
     *          haga en segundo plano (No sé si esto tiene que ver con lo multihilo).
     *      - Podría tener un storedProcedure o un procedure común y corriente que tenga la lógica esta ?
     *  - Hacer lo mismo con las imágenes de las variantes que también deben cambiar su estado
     */


    public static void main(String[] args) {
        SpringApplication.run(ProyectoTechlabSpringbootApplication.class, args);
    }

}
