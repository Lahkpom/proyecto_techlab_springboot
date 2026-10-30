package com.proyecto.proyecto_techlab_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProyectoTechlabSpringbootApplication {

    /**
     * TODO
     *  - Agregar en el flujo de eliminación de variantes que en cascada elimine las imagenes del bucket de supabase
     * TODO
     *  - Ver en SaleAdminService, si una venta no puede ser reembolsada después de aprobado el pago, que pasa entonces cuando ya pagó y quiere hacer una devolución?
     * TODO
     *  - Reveer el shippingCost del flujo de sale, se lo está recibiendo por el front pero eso debería:
     *      - Estar seteado con un costo fijo en el back
     *      - Ser dinamico dándole un flujo y una tabla en la db donde pueda modificarse
     *      - Ver si los proveedores del servicio tienen alguna api que devuelva el valor actual y calcularlo en base a eso
     * TODO
     *  - Ver el flujo de los productos.
     *      - Cuando un producto se inactiva o elimina hace lo mismo en cascada con sus variantes
     *      - Tratar de hacer que solo se cambie el estado al producto y termine la transacción, y que la cascada en las variantes se
     *          haga en segundo plano (No sé si esto tiene que ver con lo multihilo).
     *      - Podría tener un storedProcedure o un procedure común y corriente que tenga la lógica esta ?
     *  - Hacer lo mismo con las imágenes de las variantes que también deben cambiar su estado
     * TODO
     *   - Hacer un /model para user_security_tokens cuando implemente verificación de email o reseteo de clave
     *   - Ver también la lógica para que el usuario pueda modificar su correo
     * TODO
     *  - El siguiente paso en la cadena de seguridad es armar el filtro HTTP (JwtAuthenticationFilter) que intercepte cada request entrante, lea el header Authorization: Bearer <token>, extraiga el usuario con este servicio y lo cargue en el contexto de Spring Security.
     * TODO
     *  - Poner el isAnonymous en un @PreAuthorize en los endpoints de login y register para evitar que accedan usuarios con sesión activa
     */


    public static void main(String[] args) {
        SpringApplication.run(ProyectoTechlabSpringbootApplication.class, args);
    }

}
