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
     * TODO [ETAPA 2 - AUDITORÍA DE BASE DE DATOS Y JOURNALS]:
     *  1. Columnas de auditoría por tipo de tabla en calwill_y_fiorella_db:
     *    - Tablas que extienden BaseEntity (catálogo/configuración con row_status): agregar created_by y updated_by (UUID nullable FK -> users.user_id ON DELETE SET NULL) junto a created_at y updated_at.
     *    - Tablas transaccionales mutables sin row_status (ej. sale, sale_return, fiscal_receipt, cart): agregar updated_at y updated_by (sin created_by redundante).
     *    - Tablas internas append-only (ej. user_login_log, user_security_tokens, *_jn): no agregar campos de edición.
     *  2. Propagación de usuario autenticado a PostgreSQL:
     *    - Evaluar/implementar interceptor transaccional en Spring Boot con SET LOCAL app.current_user_id = '<uuid>' (NULL en procesos de sistema/schedulers/webhooks) para que los triggers BEFORE INSERT/UPDATE completen created_by, updated_by y updated_at de forma atómica.
     *  3. Rediseño de tablas Journal (*_jn) a diff en JSONB:
     *    - Reemplazar el volcado de tupla completa por almacenamiento en JSONB únicamente de los campos modificados ({ "campo": { "old": ..., "new": ... } }) cuando IS DISTINCT FROM, ignorando updates sin cambios reales de negocio y registrando el actor desde app.current_user_id.
     */

    /*
     * TODO [ETAPA 2 - REESTRUCTURACIÓN DE AUDITORÍA Y JOURNALS]:
     *  1. Estandarizar auditoría en tablas de negocio:
     *    - Entidades de BaseEntity (con row_status): created_at, created_by, updated_at, updated_by.
     *    - Entidades transaccionales mutables (sale, sale_return, fiscal_receipt, cart): created_at, updated_at, updated_by.
     *    - Excluir tablas de log/append-only (user_login_log, user_security_tokens, *_jn).
     *  2. Completar created_by / updated_by / updated_at mediante triggers leyendo current_setting('app.current_user_id', true) seteado por Spring Boot con SET LOCAL en cada transacción.
     *  3. Migrar triggers y tablas *_jn para guardar solo el diff de columnas modificadas en JSONB (old/new) en lugar de replicar toda la tupla, evitando insertar filas si solo cambió updated_at.
     */

    public static void main(String[] args) {
        SpringApplication.run(ProyectoTechlabSpringbootApplication.class, args);
    }

}
