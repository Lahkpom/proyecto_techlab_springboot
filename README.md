# Proyecto Techlab — API REST

Documentación técnica de integración para el backend y clientes React/TypeScript. Las rutas, los campos y las restricciones que siguen se contrastaron con los controladores, DTOs, enums y configuración presentes en el repositorio.

## 1. Visión general y naturaleza del proyecto

Backend de una plataforma de e-commerce y gestión comercial para clientes minoristas y mayoristas. Administra catálogo, variantes y stock, carrito y ventas; integra Mercado Pago mediante Checkout Pro y webhooks, y contempla facturación electrónica ARCA/AFIP (WSAA/WSFE).

### Stack y arquitectura

- Java 21 y Spring Boot 4.1.1.
- Spring MVC, Jackson, Spring Data JPA/Hibernate y PostgreSQL.
- Jakarta Bean Validation (Hibernate Validator), Lombok y Spring Boot DevTools.
- JWT con JJWT 0.12.6; BCrypt mediante `spring-security-crypto`.
- SDK Java de Mercado Pago 3.7.0.
- BouncyCastle 1.78.1 para CMS/PKCS#7 de WSAA.
- Supabase Storage para imágenes.

La aplicación se organiza por dominio (*Package-by-Feature*): `auth`, `user`, `product`, `variant`, `color`, `cart`, `image`, `sale`, `salereturn`, `fiscal`, más `common` para manejo compartido de errores, estado y utilidades.

## 2. Requisitos, configuración y levante local

### Requisitos

- JDK 21.
- PostgreSQL compatible con el esquema que valida Hibernate. La aplicación usa `spring.jpa.hibernate.ddl-auto=validate`: no crea ni actualiza tablas.
- Maven Wrapper incluido (`./mvnw`).
- Para los flujos correspondientes: proyecto/credenciales de Supabase Storage, Mercado Pago y ARCA/AFIP.

El perfil definido por defecto es `dev`. No se encontró configuración de perfiles separada, `Dockerfile` ni `docker-compose`; no hay una imagen o un stack Docker de aplicación mantenido por el proyecto.

### Variables de entorno

Los valores de ejemplo son formatos, no credenciales funcionales. Los nombres se obtuvieron de `application.properties` y de las propiedades inyectadas por `@Value`. Los marcados como obligatorios se interpolan sin valor alternativo en la configuración base; en desarrollo pueden usarse credenciales de prueba o endpoints mock cuando el proveedor lo admita.

| Variable | Descripción | Ejemplo / formato | Obligatoria en Dev / Prod |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/techlab` | Sí / Sí |
| `SPRING_DATASOURCE_NAME` | Usuario de la conexión JDBC | `techlab_app` | Sí / Sí |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la conexión JDBC | valor secreto | Sí / Sí |
| `SPRING_SECRET_SHA256_KEY` | Clave JWT codificada en Base64; debe tener al menos 256 bits | salida de `openssl rand -base64 32` | Sí / Sí |
| `SUPABASE_URL` | URL del proyecto Supabase | `https://<proyecto>.supabase.co` | Sí / Sí |
| `SUPABASE_SECRET_KEY` | Clave de servicio usada por Storage; no exponer al frontend | valor secreto | Sí / Sí |
| `SUPABASE_STORAGE_BUCKET` | Bucket usado para imágenes | `products` | Sí / Sí |
| `MP_ACCESS_TOKEN` | Token de acceso de Mercado Pago | token de prueba en dev | Sí / Sí |
| `MP_WEBHOOK_SECRET` | Secreto para verificar la firma de webhook | valor secreto | Sí / Sí |
| `MP_NOTIFICATION_URL` | URL pública del webhook | `https://<host>/payments/mercadopago/webhook` | Sí / Sí |
| `MP_BACK_URL_SUCCESS` | URL de retorno para pago exitoso | URL HTTPS del frontend | Sí / Sí |
| `MP_BACK_URL_FAILURE` | URL de retorno para pago fallido | URL HTTPS del frontend | Sí / Sí |
| `MP_BACK_URL_PENDING` | URL de retorno para pago pendiente | URL HTTPS del frontend | Sí / Sí |
| `ARCA_MOCK_MODE` | Activa modo simulado de la integración fiscal | `true` o `false` | Sí / Sí |
| `ARCA_CUIT` | CUIT emisor usado por WSFE | dígitos de CUIT | Sí / Sí |
| `ARCA_PUNTO_VENTA` | Punto de venta fiscal | entero, por ejemplo `1` | Sí / Sí |
| `ARCA_CERT_PATH` | Ruta al certificado de WSAA | ruta accesible desde el proceso | Sí / Sí |
| `ARCA_KEY_PATH` | Ruta a la clave privada de WSAA | ruta accesible desde el proceso | Sí / Sí |
| `ARCA_WSAA_URL` | URL del servicio de autenticación | URL WSAA de homologación o producción | Sí / Sí |
| `ARCA_WSFE_URL` | URL del servicio de facturación | URL WSFE de homologación o producción | Sí / Sí |
| `ARCA_ANONYMOUS_INVOICE_LIMIT` | Umbral para identificar compras anónimas que requieren identificación fiscal | decimal, por ejemplo `340000.00` | Sí / Sí |

El JWT de acceso dura `900000` ms (15 minutos), fijado en `application.properties`. Los refresh tokens duran 7 días según `AuthService`; esta duración no es una variable de entorno. La purga de ventas vencidas tiene valores de configuración predeterminados en properties: diariamente a las 03:00, zona `America/Argentina/Buenos_Aires`, TTL de 48 horas.

### Ejecución local

1. Configurar las variables de entorno de la tabla y disponer del esquema PostgreSQL requerido.
2. Desde la raíz:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Para compilar el JAR:

   ```bash
   ./mvnw clean package
   java -jar target/proyecto_techlab_springboot-0.0.1-SNAPSHOT.jar
   ```

No se declara `server.port`; Spring Boot usa su puerto predeterminado (8080), salvo configuración externa.

### Uso de Docker

El repositorio no incluye definición Docker para ejecutar la aplicación. Como ayuda local, se puede iniciar solamente PostgreSQL en un contenedor (contraseña de ejemplo exclusivamente local):

```bash
docker run --name techlab-postgres \
  -e POSTGRES_DB=techlab \
  -e POSTGRES_USER=techlab_app \
  -e POSTGRES_PASSWORD=dev-only \
  -p 5432:5432 \
  -d postgres:16
```

Configurar `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/techlab`, `SPRING_DATASOURCE_NAME=techlab_app` y `SPRING_DATASOURCE_PASSWORD=dev-only`, además de las variables de integración requeridas, y ejecutar la aplicación con Maven en el host. Esto no inicializa el esquema de negocio ni constituye un despliegue Docker de la aplicación.

## 3. Convenciones globales de la API

### Base URL, autenticación y seguridad

Los controladores declaran sus rutas sin prefijo global. Por ejemplo, localmente la ruta anotada `/products` se invoca como `http://localhost:8080/products`. `application.properties` contiene `spring.mvc.api-prefix=/api`, pero en el código inspeccionado no hay configuración que consuma esa propiedad; verificar el prefijo del entorno antes de agregar `/api` desde el frontend.

Cuando un endpoint requiere token, enviarlo como:

```http
Authorization: Bearer <accessToken>
```

`/auth/login` y `/auth/refresh` devuelven un `AuthResponse` con access token, refresh token, tipo `Bearer`, vencimiento y usuario. El access token incluye claims `userId` y `role`. El refresh token opaco se guarda en forma de hash; su uso lo rota y revoca el anterior. `/auth/logout` revoca el refresh token recibido si se proporciona.

**Límite importante de la implementación actual:** `SecurityConfig` solo registra `BCryptPasswordEncoder`. No se configura `SecurityFilterChain`, filtro JWT ni reglas de autorización por rol. Las anotaciones `@PreAuthorize` de varios controladores están comentadas. En consecuencia:

- No hay protección efectiva por `CUSTOMER`, `WHOLESALER`, `ADMIN` o `SUPER_ADMIN` en el framework.
- Algunos controladores llaman manualmente a `SecurityContextUtil` y exigen un JWT válido, pero no verifican el rol del claim.
- Hay rutas `/admin/...`, escritura de variantes/imágenes y operaciones de usuario que hoy no aplican autenticación ni autorización en el controlador.
- `/auth/register/wholesalers` está comentada como ruta prevista para administradores, pero no aplica esa restricción.
- `UserController` deja comentado el control de que un usuario solo consulte/actualice sus propios datos.

Por tanto, las etiquetas “cliente”, “administrativo” y “público” que se usan abajo describen el propósito de las rutas, no un permiso por rol garantizado por el backend. No tratar estas rutas como protegidas hasta habilitar y probar la autorización.

### Paginación y ordenamiento

Los endpoints paginados aceptan parámetros estándar de Spring Data:

```text
?page=0&size=20&sort=createdAt,desc
```

`page` es base cero; `size` es el tamaño de página; `sort` admite propiedad y dirección (`asc`/`desc`). Si no se especifica, los controladores usan `@PageableDefault` con tamaño 20 para productos/ventas administrativas, 15 para devoluciones administrativas y 10 para historiales de cliente. No se fija un orden predeterminado en los controladores.

El contrato `Page<T>` aporta los campos `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last` y `empty`. Spring Data también puede serializar metadatos de implementación (`pageable`, `sort`, `numberOfElements`); el backend no transforma la página a un DTO propio.

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true,
  "empty": true,
  "numberOfElements": 0,
  "sort": { "empty": true, "sorted": false, "unsorted": true },
  "pageable": {}
}
```

### Manejo de errores

El manejador global devuelve `ErrorResponse` con esta forma:

```json
{
  "timestamp": "2026-11-11T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "detail": "El nombre no puede estar vacío.",
  "path": "/products"
}
```

`timestamp` es `Instant`, `status` un entero, `error` un código textual, `detail` el mensaje y `path` la URI solicitada. Para validaciones se devuelve solo el primer error de campo.

| HTTP | Código(s) de error | Excepciones / condición implementada |
|---|---|---|
| 400 | `VALIDATION_FAILED`, `INVALID_PARAMETER`, `INVALID_STATUS_TRANSITION`, `RETURN_QUANTITY_EXCEEDED`, `TAX_IDENTIFICATION_REQUIRED` | Bean Validation cuando el cuerpo usa `@Valid`; tipo/formato incorrecto de path/query; `InvalidStatusTransitionException` (incluye transición inválida de devolución); cantidad a devolver mayor a la disponible; falta identificación fiscal requerida en checkout. |
| 401 | `UNAUTHORIZED`, `INVALID_WEBHOOK_SIGNATURE` | Credenciales, sesión o JWT inválidos; firma de webhook inválida. `UNAUTHORIZED` es también el código usado por `AccessDeniedException`. |
| 403 | `EMAIL_NOT_VERIFIED`, `PASSWORD_EXPIRED`, `UNAUTHORIZED` | Cuenta sin email verificado, contraseña expirada o `AccessDeniedException`. |
| 404 | `RESOURCE_NOT_FOUND` | Subclases de `ResourceNotFoundException`, como recurso, usuario, producto, variante, venta, devolución o comprobante inexistente. |
| 409 | `RESOURCE_ALREADY_EXISTS`, `INSUFFICIENT_STOCK`, `SALE_NOT_ELIGIBLE_FOR_RETURN`, `ACTIVE_RETURN_ALREADY_EXISTS` | Duplicados; stock insuficiente; venta no elegible para devolución; reclamo activo existente. |
| 422 | `INVALID_REORDER_SEQUENCE`, `MINIMUM_WHOLESALE_QUANTITY_NOT_MET`, `INVALID_STATUS_TRANSITION`, `EMPTY_SALE_ITEMS`, `PAYMENT_AMOUNT_MISMATCH`, `RETURN_WINDOW_EXPIRED` | Secuencia de imágenes inválida; mínimo mayorista; transición comercial de venta no permitida; compra sin ítems; importe de pago discrepante; devolución fuera de plazo. |
| 502 | `STORAGE_SERVICE_ERROR`, `MERCADO_PAGO_INTEGRATION_ERROR` | Fallo al comunicarse con Supabase Storage o Mercado Pago. |
| 503 | — | No hay un handler específico que responda 503. |

También se implementan `410 GONE` (`RESOURCE_NO_LONGER_AVAILABLE`) para recursos inactivos/no disponibles, `423 LOCKED` (`ACCOUNT_LOCKED`) por bloqueo de login y `500 INTERNAL_SERVER_ERROR` para excepciones no mapeadas. En particular, los errores de ARCA y algunas `IllegalArgumentException`/`IllegalStateException` caen en el handler genérico y no tienen código HTTP de integración específico. El handler genérico actualmente devuelve `ex.getMessage()` en `detail`; no debe asumirse que ese detalle es seguro para producción.

### Enums de dominio

Los valores se envían como strings JSON, respetando mayúsculas y guiones bajos:

| Enum | Valores exactos |
|---|---|
| `UserRole` | `SUPER_ADMIN`, `ADMIN`, `WHOLESALER`, `CUSTOMER` |
| `TaxIdType` | `DNI`, `CUIL`, `CUIT`, `PASAPORTE` |
| `Category` | `CALZADO` |
| `RowStatus` | `ACTIVE`, `INACTIVE`, `DELETED` |
| `NumericSize` | `T_18`, `T_19`, `T_20`, `T_21`, `T_22`, `T_23`, `T_24`, `T_25`, `T_26`, `T_35`, `T_36`, `T_37`, `T_38`, `T_39`, `T_40`, `T_41`, `T_42`, `T_35_36`, `T_37_38`, `T_39_40`, `T_41_42`, `T_43_44`, `T_45_46` |
| `TargetGender` | `NINIAS`, `NINIOS`, `HOMBRES`, `MUJERES`, `UNISEX` |
| `SaleStatus` | `PENDIENTE`, `PREPARACION`, `ENVIADO`, `ENTREGADO`, `CANCELADO`, `REEMBOLSADO`, `AUDITORIA`, `RECLAMO_ABIERTO`, `CERRADO_CON_DEVOLUCION` |
| `PaymentStatus` | `PENDIENTE`, `APROBADO`, `RECHAZADO`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO` |
| `PaymentMethod` | `MERCADO_PAGO`, `TRANSFERENCIA`, `EFECTIVO` |
| `ReturnStatus` | `SOLICITADA`, `EN_TRANSITO`, `RECIBIDO_EN_CONTROL`, `APROBADA`, `RECHAZADA`, `CANCELADA` |
| `ReturnReason` | `ARREPENTIMIENTO_LEGAL`, `CAMBIO_TALLE`, `DEFECTO_FABRICACION` |
| `ReturnResolution` | `REEMBOLSO_DINERO`, `CAMBIO_PRODUCTO`, `CREDITO_TIENDA` |
| `FiscalReceiptType` | `FACTURA_C`, `NOTA_DEBITO_C`, `NOTA_CREDITO_C` |

**Inconsistencia a atender:** `UserRole` declara `SUPER_ADMIN`, mientras varios comentarios `@PreAuthorize` escriben `SUPERADMIN`. Al habilitar autorización, alinear los nombres con el enum.

## 4. Referencia detallada de endpoints

Los contratos de campos (`Request`/`Response`) y ejemplos se agrupan después del catálogo de rutas para evitar repetir la misma estructura cuando más de una operación usa el mismo DTO. Los parámetros marcados como “opcional” pueden omitirse; las restricciones de cuerpo aplican solo cuando el controlador invoca `@Valid`.

### Auth

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /auth/login` | Verifica credenciales, registra intento y crea sesión; bloquea después de 5 fallos consecutivos durante 15 minutos. | Pública; sin rol requerido. | Ninguno. | `LoginRequest` (**@Valid**). | `200 AuthResponse`. | `400` validación; `401` credenciales inválidas; `403` email sin verificar o contraseña expirada; `423` cuenta bloqueada. |
| `POST /auth/refresh` | Rota el refresh token: invalida el anterior y emite un nuevo par. Refresh token válido por 7 días. | Pública; presenta el refresh token en el cuerpo. | Ninguno. | `RefreshTokenRequest` (**@Valid**). | `200 AuthResponse`. | `400` validación; `401` token inexistente, expirado o revocado. |
| `POST /auth/register` | Registra un cliente con rol `CUSTOMER`. | Pública. | Ninguno. | `UserCreateRequest` (**@Valid**). | `201 UserResponse`. | `400` validación; `409` email duplicado. |
| `POST /auth/register/wholesalers` | Registra un mayorista con rol `WHOLESALER` y compañía. | Prevista para administradores, pero esa autorización está comentada: actualmente no se verifica rol. | Ninguno. | `WholesalerCreateRequest` (**@Valid**). | `201 UserResponse`. | `400` validación; `409` email duplicado. |
| `POST /auth/logout` | Revoca el refresh token si se envía. Sin cuerpo, responde sin cambios. | Pública; no exige access token. | Ninguno. | `RefreshTokenRequest` opcional, **sin `@Valid`**. | `204` sin cuerpo. | No hay error específico si el token no existe/no está activo. |

### Users

Rutas de usuario; aunque funcionalmente se orientan al perfil propio, los métodos no verifican que el `id` corresponda al JWT ni requieren rol.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /users/{id}` | Consulta un usuario por UUID. | Sin autenticación/autorización en el controlador; comentario pendiente de limitar a propio usuario. | `id`: UUID, requerido. | — | `200 UserResponse`. | `400` UUID inválido; `404` usuario inexistente. |
| `PUT /users/{id}` | Actualiza nombre, teléfono e identificación fiscal. | Sin verificación de identidad/rol. | `id`: UUID, requerido. | `UserUpdateRequest` (**@Valid**). | `200 UserResponse`. | `400` validación/UUID; `404` usuario inexistente. |
| `PATCH /users/{id}/password` | Actualiza contraseña. | Sin verificación de identidad/rol. | `id`: UUID, requerido. | `PasswordUpdateRequest` (**@Valid**). | `200 UserResponse`. | `400` validación/UUID; `404` usuario inexistente. |
| `PATCH /users/{id}/status` | Cambia estado de fila. | Sin verificación de identidad/rol. | `id`: UUID, requerido. | `StatusUpdateRequest` (**@Valid**). | `200 UserResponse`. | `400` validación/UUID; `404` usuario inexistente. |

### Products

#### Públicos / cliente

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /products` | Lista productos públicos paginados. | Público; solo productos que el servicio considere públicos. | Query `page`, `size`, `sort` (Pageable; default size 20). | — | `200 Page<ProductResponse>`. | `400` parámetro inválido. |
| `GET /products/{id}` | Consulta un producto público. | Público. | `id`: UUID, requerido. | — | `200 ProductResponse`. | `400` UUID inválido; `404` producto inexistente; `410` producto no disponible. |

#### Administrativos (`/admin/...`)

No hay verificación efectiva de rol ADMIN en estos métodos; `@PreAuthorize` del controlador está comentado.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /admin/products` | Crea producto (la categoría que asigna actualmente el servicio es `CALZADO`). | No protegido actualmente por rol. | Ninguno. | `ProductCreateRequest` (**@Valid**). | `201 ProductResponse`. | `400` validación. |
| `GET /admin/products` | Lista productos para administración, paginados. | Sin autorización efectiva. | Pageable; default size 20. | — | `200 Page<ProductResponse>`. | `400` parámetro inválido. |
| `GET /admin/products/{id}` | Consulta producto por UUID. | Sin autorización efectiva. | `id`: UUID, requerido. | — | `200 ProductResponse`. | `400` UUID inválido; `404` producto inexistente. |
| `PUT /admin/products/{id}` | Reemplaza campos editables del producto. | Sin autorización efectiva. | `id`: UUID, requerido. | `ProductUpdateRequest` (**@Valid**). | `200 ProductResponse`. | `400` validación/UUID; `404` producto inexistente. |
| `PATCH /admin/products/{id}/status` | Cambia estado de fila; al pasar de `ACTIVE` a `INACTIVE` o `DELETED`, el servicio también inactiva o elimina lógicamente sus variantes. | Sin autorización efectiva. | `id`: UUID, requerido. | `StatusUpdateRequest` (**@Valid**). | `200 ProductResponse`. | `400` validación/UUID; `404` producto inexistente. |

### Variants

Las operaciones de escritura están pensadas para administración, pero los `@PreAuthorize` están comentados y la ruta no tiene prefijo `/admin`.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /variants/by-product/{productId}` | Lista variantes de un producto. | Público; no valida rol. | `productId`: UUID, requerido. Query `includeInactive`: boolean, opcional, default `false`. | — | `200 List<VariantResponse>`. | `400` UUID/boolean inválido; `404` producto inexistente según servicio. |
| `GET /variants/{id}` | Consulta variante por ID. | Público; no valida rol. | `id`: Long, requerido. | — | `200 VariantResponse`. | `400` ID inválido; `404` variante inexistente. |
| `POST /variants` | Crea una variante y asocia producto/color, talla, SKU, precio y stock. | Intención administrativa; no protegido actualmente. | Ninguno. | `VariantCreateRequest` (**@Valid**). | `201 VariantResponse`. | `400` validación; `404` producto/color inexistente; `409` SKU o combinación duplicada. |
| `PUT /variants/{id}` | Actualiza atributos, precio y stock de una variante. | Intención administrativa; no protegido actualmente. | `id`: Long, requerido. | `VariantUpdateRequest` (**@Valid**). | `200 VariantResponse`. | `400` validación/ID; `404` variante/color inexistente; `409` SKU/combinación duplicada. |
| `PATCH /variants/{id}/status` | Cambia `RowStatus`. | Intención administrativa; no protegido actualmente. | `id`: Long, requerido. | `StatusUpdateRequest` (**@Valid**). | `200 VariantResponse`. | `400` validación/ID; `404` variante inexistente. |

### Colors

#### Públicos / cliente

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /colors` | Lista colores; el servicio puede incluir inactivos según flag. | Público. | Query `includeInactive`: boolean, opcional, default `false`. | — | `200 List<ColorResponse>`. | `400` boolean inválido. |
| `GET /colors/{id}` | Consulta color por ID. | Público. | `id`: Long, requerido. | — | `200 ColorResponse`. | `400` ID inválido; `404` color inexistente. |

#### Administrativos (`/admin/...`)

`@PreAuthorize` está comentado: el path no significa que exista autorización en runtime.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /admin/colors` | Crea color. | Sin autorización efectiva. | Ninguno. | `ColorCreateRequest` (**@Valid**). | `201 ColorResponse`. | `400` validación; `409` nombre o código duplicado. |
| `PUT /admin/colors/{id}` | Actualiza nombre, descripción y código. | Sin autorización efectiva. | `id`: Long, requerido. | `ColorUpdateRequest` (**@Valid**). | `200 ColorResponse`. | `400` validación/ID; `404` color inexistente; `409` nombre/código duplicado. |
| `PATCH /admin/colors/{id}/status` | Cambia estado. | Sin autorización efectiva. | `id`: Long, requerido. | `StatusUpdateRequest` (**@Valid**). | `200 ColorResponse`. | `400` validación/ID; `404` color inexistente. |

### Images

Flujo de alta de imagen: pedir URL firmada, cargar el binario directamente a Supabase con `PUT` (esa carga no es un endpoint de esta API) y luego registrar la URL en la API. El controlador declara que es administrativo, pero la anotación de autorización está comentada.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /variants/{variantId}/images/presigned-url` | Genera URLs de subida temporal y pública (la URL firmada tiene vigencia de 10 minutos). | Intención administrativa; actualmente sin control de rol/token. | `variantId`: Long, requerido. | `ImagePresignedUrlRequest` (**@Valid**). | `200 ImagePresignedUrlResponse`. | `400` validación/ID; `404` variante inexistente; `502` si Storage responde sin URL firmada; excepciones de transporte no mapeadas pueden producir `500`. |
| `POST /variants/{variantId}/images` | Persiste referencia a imagen ya subida; la variante debe estar activa. | Intención administrativa; actualmente sin control efectivo. | `variantId`: Long, requerido. | `ImageCreateRequest` (**@Valid**). | `201 ImageResponse`. | `400` validación/ID; `404` variante inexistente; `410` variante no disponible. |
| `GET /variants/{variantId}/images` | Lista galería ordenada por imagen principal y orden de display. | Sin control de rol en el controlador. | `variantId`: Long, requerido. | — | `200 List<ImageResponse>`. | `400` ID inválido; `404` variante inexistente. |
| `PATCH /variants/{variantId}/images/{imageId}/primary` | Marca imagen principal y desmarca la anterior. | Intención administrativa; actualmente sin control efectivo. | `variantId`, `imageId`: Long, requeridos. | — | `200 ImageResponse`. | `400` ID inválido; `404` variante/imagen inexistente. |
| `PATCH /variants/{variantId}/images/reorder` | Sincroniza el orden completo de imágenes de una variante. | Intención administrativa; actualmente sin control efectivo. | `variantId`: Long, requerido. | `ImageReorderRequest` (**@Valid**). La lista debe incluir IDs únicos que correspondan exactamente a la galería. | `200 List<ImageResponse>`. | `400` validación/ID; `404` variante/imágenes inexistentes; `422` secuencia duplicada o incompleta. |
| `DELETE /variants/{variantId}/images/{imageId}` | Elimina el registro y solicita borrar el archivo de Storage. | Intención administrativa; actualmente sin control efectivo. | `variantId`, `imageId`: Long, requeridos. | — | `204` sin cuerpo. | `400` ID inválido; `404` imagen/variante inexistente. El servicio captura y registra los errores al borrar de Storage, por lo que la API puede responder `204` aunque el archivo no se haya eliminado. |

### Cart

Todos los endpoints resuelven el usuario mediante `SecurityContextUtil` y exigen un JWT válido con claim `userId`. No verifican que el rol sea `CUSTOMER` o `WHOLESALER`.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /cart` | Obtiene el carrito del usuario del token; puede purgar variantes inactivas/sin stock y ajustar cantidades al stock actual. | JWT requerido manualmente; sin control de rol. | Ninguno. | — | `200 CartResponse`. | `401` JWT ausente/inválido; `404` usuario inexistente. |
| `POST /cart/item` | Agrega producto/variante al carrito. | JWT requerido manualmente. | Ninguno. | `CartItemRequest` (**@Valid**). | `201 CartResponse`. | `400` validación; `401`; `404` variante; `409` stock insuficiente; `410` variante no disponible. |
| `PATCH /cart/item/{variantId}` | Actualiza cantidad del ítem. | JWT requerido manualmente. | `variantId`: Long, requerido. | `CartItemUpdateRequest` (**@Valid**). | `200 CartResponse`. | `400` validación/ID; `401`; `404` ítem/variante; `409` stock; `410` variante no disponible. |
| `DELETE /cart/item/{variantId}` | Elimina un ítem. | JWT requerido manualmente. | `variantId`: Long, requerido. | — | `204` sin cuerpo. | `400` ID inválido; `401`; `404` ítem inexistente. |
| `DELETE /cart` | Vacía el carrito. | JWT requerido manualmente. | Ninguno. | — | `204` sin cuerpo. | `401` JWT ausente/inválido. |

### Sales & Webhooks

#### Públicos / cliente

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /sales/checkout` | Crea venta con los ítems incluidos en el request; valida stock/estado, calcula importes según política comercial, inicia pago y puede requerir identificación fiscal. | Checkout invitado o autenticado opcionalmente. Un token no válido se trata como invitado en esta resolución opcional. | Ninguno. | `CheckoutRequest` (**@Valid**); aunque `items` no lleva `@NotEmpty` y el comentario del DTO dice que puede omitirse para usuarios autenticados, el servicio rechaza `items` ausente o vacío para cualquier comprador. | `201 SaleResponse`. | `400` validación/identificación fiscal requerida; `404` variante; `409` stock; `422` sin ítems o mínimo mayorista; `502` Mercado Pago. |
| `POST /sales/me/{saleId}/retry-mp` | Reintenta iniciar pago Mercado Pago para una venta propia pendiente y cuyo pago esté pendiente o rechazado. | JWT requerido manualmente; no hay verificación de rol. | `saleId`: UUID, requerido. | — | `200 SaleResponse`. | `400` UUID inválido; `401`; `404` venta; `403` no pertenece al usuario; `422` método/estado no elegible; `502` Mercado Pago. |
| `GET /sales/track/{trackingToken}` | Consulta una venta mediante token de seguimiento. | Público con token de seguimiento. | `trackingToken`: UUID, requerido. | — | `200 SaleResponse`. | `400` UUID inválido; `404` venta/token inexistente. |
| `GET /sales/me` | Historial paginado del cliente. | JWT requerido manualmente; no hay verificación de rol. | Pageable, default size 10. | — | `200 Page<SaleSummaryResponse>`. | `400` query inválida; `401`. |
| `GET /sales/me/{saleId}` | Detalle de una venta del cliente. | JWT requerido manualmente; el servicio limita por usuario. | `saleId`: UUID, requerido. | — | `200 SaleResponse`. | `400` UUID inválido; `401`; `404` venta; `403` no pertenece al usuario. |
| `PATCH /sales/me/{saleId}/cancel` | Cancela venta propia en estado `PENDIENTE` o `PREPARACION` y restituye el stock. | JWT requerido manualmente; no hay verificación de rol. | `saleId`: UUID, requerido. | — | `200 SaleResponse`. | `400` UUID inválido; `401`; `404`; `403` no pertenece al usuario; `422` estado no cancelable. |

#### Administrativos (`/admin/...`)

Las búsquedas/detalles no exigen JWT en sus métodos; las tres mutaciones sí requieren un token manual, pero no comprueban rol.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /admin/sales` | Búsqueda paginada por estado de venta/pago, email y rango temporal. | Sin autorización efectiva. | Opcionales: `saleStatus` (`SaleStatus`), `paymentStatus` (`PaymentStatus`), `customerEmail` (String), `startDate`/`endDate` (`Instant`, ISO date-time), Pageable (default size 20). | — | `200 Page<SaleSummaryResponse>`. | `400` enum/fecha/page inválidos. |
| `GET /admin/sales/{saleId}` | Detalle de venta. | Sin autorización efectiva. | `saleId`: UUID, requerido. | — | `200 SaleResponse`. | `400` UUID inválido; `404` venta inexistente. |
| `PATCH /admin/sales/{saleId}/status` | Cambia estado logístico según máquina de estados. | Requiere JWT manual; sin autorización por rol. | `saleId`: UUID, requerido. | `SaleStatusUpdateRequest` (**@Valid**). | `200 SaleResponse`. | `400` validación; `401`; `404`; `422` transición de venta no permitida. |
| `PATCH /admin/sales/{saleId}/payment-status` | Conciliación manual del estado de pago. | Requiere JWT manual; sin autorización por rol. | `saleId`: UUID, requerido. | `PaymentStatusUpdateRequest` (**@Valid**). | `200 SaleResponse`. | `400` validación; `401`; `404`; `422` transición de pago no permitida. |
| `PATCH /admin/sales/{saleId}/internal-notes` | Crea/edita notas operativas internas. | Requiere JWT manual; sin autorización por rol. | `saleId`: UUID, requerido. | `SaleInternalNoteRequest` (**@Valid**). | `200 SaleResponse`. | `400` validación; `401`; `404`. |

#### Webhooks externos

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /payments/mercadopago/webhook` | Procesa notificación de pago y actualiza la venta; puede disparar facturación fiscal. | Integración externa; la firma se valida en el servicio. No requiere JWT de usuario. | Header opcional `x-signature`; query opcionales `topic` (String), `id` (String). | `Map<String,Object>` opcional; se leen `action`, `type` y `data.id`. **Sin `@Valid`**. | `200` sin cuerpo después del procesamiento aceptado. | `401 INVALID_WEBHOOK_SIGNATURE` por firma inválida; errores upstream no tratados de otra forma pueden caer en `500`. |

### Sale Returns

#### Públicos / cliente

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `POST /returns/orders/{saleId}` | Crea reclamo de devolución asociado a una venta del usuario. | JWT requerido manualmente; el servicio valida titularidad. | `saleId`: UUID, requerido. | `SaleReturnCreateRequest` (**@Valid**). | `201 SaleReturnResponse`. | `400` validación o cantidad excedida; `401`; `403` no corresponde; `404` venta; `409` venta no elegible/reclamo activo; `422` fuera de plazo o transición de venta no permitida; `500` si un `saleItemId` no pertenece a la orden (el servicio lanza `IllegalArgumentException`, que cae al handler genérico). |
| `POST /returns/guest/{trackingToken}` | Crea reclamo para compra invitada usando tracking de venta. | Público con token de seguimiento. | `trackingToken`: UUID, requerido. | `SaleReturnCreateRequest` (**@Valid**). | `201 SaleReturnResponse`. | `400` validación o cantidad excedida; `404` venta/token; `409` venta no elegible/reclamo activo; `422` ventana vencida o transición de venta no permitida; `500` si un `saleItemId` no pertenece a la orden (el servicio lanza `IllegalArgumentException`, que cae al handler genérico). |
| `GET /returns/track/{returnTrackingToken}` | Consulta devolución mediante su token propio. | Público con token de seguimiento. | `returnTrackingToken`: UUID, requerido. | — | `200 SaleReturnResponse`. | `400` UUID inválido; `404` devolución/token inexistente. |
| `GET /returns/me` | Historial paginado de devoluciones del usuario. | JWT requerido manualmente; no hay control de rol. | Pageable, default size 10. | — | `200 Page<SaleReturnSummaryResponse>`. | `400` query inválida; `401`. |

#### Administrativos (`/admin/...`)

Las rutas tienen intención administrativa/operativa, pero la anotación de rol está comentada. Las mutaciones requieren JWT manual, sin verificar rol.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /admin/returns` | Búsqueda paginada por estado, motivo, email y fechas. | Sin autorización efectiva. | Opcionales: `returnStatus` (`ReturnStatus`), `returnReason` (`ReturnReason`), `customerEmail` (String), `startDate`/`endDate` (`Instant`, ISO date-time); Pageable default size 15. | — | `200 Page<SaleReturnSummaryResponse>`. | `400` enum/fecha/page inválidos. |
| `GET /admin/returns/{returnId}` | Detalle del expediente. | Sin autorización efectiva. | `returnId`: UUID, requerido. | — | `200 SaleReturnResponse`. | `400` UUID inválido; `404` devolución inexistente. |
| `PATCH /admin/returns/{returnId}/status` | Avanza el estado logístico y/o registra guía/notas. | JWT requerido manual; rol no verificado. | `returnId`: UUID, requerido. | `SaleReturnStatusUpdateRequest` (**@Valid**). | `200 SaleReturnResponse`. | `400` validación/transición; `401`; `404` devolución. |
| `POST /admin/returns/{returnId}/inspect` | Registra inspección general y dictamen por prenda; el servicio puede reingresar stock aprobado. | JWT requerido manual; rol no verificado. | `returnId`: UUID, requerido. | `SaleReturnInspectionRequest` (**@Valid**). | `200 SaleReturnResponse`. | `400` validación/transición; `401`; `404` devolución/ítem. |
| `POST /admin/returns/{returnId}/process-refund` | Ejecuta reembolso de pasarela o asienta conciliación manual. | JWT requerido manual; rol no verificado. | `returnId`: UUID, requerido. | `SaleReturnRefundRequest` opcional y **@Valid** si se envía. | `200 SaleReturnResponse`. | `400` validación/transición; `401`; `404`; los rechazos de reglas lanzados como `IllegalStateException`/`IllegalArgumentException` terminan en `500` genérico; errores Mercado Pago mapeados a `502`. |

### Fiscal Receipts

El controlador solo expone consultas. La emisión fiscal se integra en servicios/flujo de pagos y no tiene endpoint REST de emisión en este módulo.

#### Administrativos (`/admin/...`)

`@PreAuthorize` está comentado; actualmente no hay protección de rol efectiva.

| Método y ruta | Propósito | Seguridad / roles | Path y query | Request body | Éxito | Errores específicos |
|---|---|---|---|---|---|---|
| `GET /admin/fiscal/sales/{saleId}` | Lista comprobantes fiscales asociados a una venta. | Sin autorización efectiva. | `saleId`: UUID, requerido. | — | `200 List<FiscalReceiptResponse>`; si no hay comprobantes, la lista es vacía. | `400` UUID inválido; `404` venta inexistente. |
| `GET /admin/fiscal/{fiscalReceiptId}` | Consulta comprobante por UUID. | Sin autorización efectiva. | `fiscalReceiptId`: UUID, requerido. | — | `200 FiscalReceiptResponse`. | `400` UUID inválido; `404` comprobante inexistente. |

## 5. Contratos de DTO y ejemplos

Los siguientes contratos son el detalle de campos de los cuerpos y respuestas referenciados en las tablas anteriores. En `Requerido`, “Sí” significa que hay una restricción de presencia (`@NotNull`, `@NotBlank` o `@NotEmpty`) en el DTO. “No” significa nullable desde Bean Validation; no necesariamente que el servicio permita omitirlo en todo contexto.

### DTOs de request

#### `LoginRequest`

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `email` | string | Sí | `@NotBlank`, `@Email`. |
| `password` | string | Sí | `@NotBlank`. |

```json
{"email":"cliente@example.com","password":"<contraseña>"}
```

#### `RefreshTokenRequest`

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `refreshToken` | string | Sí | `@NotBlank`. |

```json
{"refreshToken":"<refresh-token>"}
```

#### `UserCreateRequest`

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `name` | string | Sí | `@NotBlank`, máximo 255. |
| `email` | string | Sí | `@NotBlank`, `@Email`, máximo 255. |
| `password` | string | Sí | `@NotBlank`, máximo 255. |
| `phone` | string | No | Si se informa, regex `ValidationUtil.PHONE_REGEX`: exactamente 10 dígitos. |
| `taxIdType` | `TaxIdType` | No | Valores enumerados; sin anotación de presencia. |
| `taxIdNumber` | string | No | Sin restricción declarada. |

```json
{"name":"Ana Pérez","email":"ana@example.com","password":"<contraseña>","phone":"1123456789","taxIdType":"DNI","taxIdNumber":"30123456"}
```

#### `WholesalerCreateRequest`

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `name` | string | Sí | `@NotBlank`, máximo 255. |
| `email` | string | Sí | `@NotBlank`, `@Email`, máximo 255. |
| `password` | string | Sí | `@NotBlank`, máximo 255. |
| `phone` | string | No | Si se informa, exactamente 10 dígitos por `PHONE_REGEX`. |
| `companyName` | string | Sí | `@NotBlank`, máximo 255. |
| `taxIdType` | `TaxIdType` | Sí | `@NotNull`. |
| `taxIdNumber` | string | Sí | `@NotBlank`. |

```json
{"name":"Juan López","email":"juan@comercio.example","password":"<contraseña>","phone":"1123456789","companyName":"Comercial Ejemplo","taxIdType":"CUIT","taxIdNumber":"20123456789"}
```

#### `UserUpdateRequest`, `PasswordUpdateRequest`, `StatusUpdateRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `UserUpdateRequest.name` | string | Sí | `@NotBlank`, máximo 255. |
| `UserUpdateRequest.phone` | string | No | Si se informa, exactamente 10 dígitos. |
| `UserUpdateRequest.taxIdType` | `TaxIdType` | No | Sin anotación de presencia. |
| `UserUpdateRequest.taxIdNumber` | string | No | Sin restricción declarada. |
| `PasswordUpdateRequest.password` | string | Sí | `@NotNull`, máximo 255; no exige `@NotBlank`. |
| `StatusUpdateRequest.rowStatus` | `RowStatus` | Sí | `@NotNull`. |

```json
{"name":"Ana Pérez","phone":"1123456789","taxIdType":"DNI","taxIdNumber":"30123456"}
```

```json
{"password":"<nueva-contraseña>"}
```

```json
{"rowStatus":"ACTIVE"}
```

#### `ProductCreateRequest` y `ProductUpdateRequest`

Los dos DTOs tienen los mismos campos y restricciones.

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `name` | string | Sí | `@NotBlank`, máximo 255. |
| `shortDesc` | string | Sí | `@NotBlank`, máximo 255. |
| `longDesc` | string | Sí | `@NotBlank`, máximo 5000. |

```json
{"name":"Zapatilla Urbana","shortDesc":"Modelo liviano","longDesc":"Descripción completa del producto."}
```

#### `VariantCreateRequest` y `VariantUpdateRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `VariantCreateRequest.productId` | UUID | Sí | `@NotNull`. |
| `colorId` | Long | Sí | `@NotNull`. |
| `size` | `NumericSize` | Sí | `@NotNull`. |
| `targetGender` | `TargetGender` | Sí | `@NotNull`. |
| `desc` | string | Sí | `@NotBlank`, máximo 255. |
| `sku` | string | Sí | `@NotBlank`, máximo 100. |
| `price` | decimal (`BigDecimal`) | Sí | `@NotNull`, `@DecimalMin("0.0")`, inclusive. |
| `stock` | integer | Sí | `@NotNull`, `@Min(0)`. |
| `VariantUpdateRequest` omite `productId`; los demás campos tienen las mismas reglas. | — | — | — |

```json
{"productId":"550e8400-e29b-41d4-a716-446655440000","colorId":12,"size":"T_38","targetGender":"MUJERES","desc":"Cuero negro","sku":"URB-NEG-38","price":45999.90,"stock":20}
```

```json
{"colorId":12,"size":"T_38","targetGender":"MUJERES","desc":"Cuero negro","sku":"URB-NEG-38","price":45999.90,"stock":20}
```

#### `ColorCreateRequest` y `ColorUpdateRequest`

| Campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `name` | string | Sí | `@NotBlank`, máximo 50. |
| `desc` | string | Sí | `@NotBlank`, máximo 2000. |
| `code` | string | Sí | `@NotBlank`, patrón `^#[A-Fa-f0-9]{6}$`. |

```json
{"name":"Negro","desc":"Negro clásico","code":"#000000"}
```

#### `ImagePresignedUrlRequest`, `ImageCreateRequest`, `ImageReorderRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `ImagePresignedUrlRequest.originalFileName` | string | Sí | `@NotBlank`. |
| `ImagePresignedUrlRequest.contentType` | string | Sí | `@NotBlank`, uno de `image/jpeg`, `image/png`, `image/webp`. |
| `ImageCreateRequest.imageUrl` | string | Sí | `@NotBlank`, formato URL según Hibernate `@URL`. |
| `ImageCreateRequest.isPrimary` | boolean | Sí | `@NotNull`. |
| `ImageReorderRequest.orderedImageIds` | array de Long | Sí | `@NotEmpty`; cada elemento `@NotNull`; la regla de unicidad/completitud se valida en servicio. |

```json
{"originalFileName":"frente.webp","contentType":"image/webp"}
```

```json
{"imageUrl":"https://storage.example.com/products/variante/frente.webp","isPrimary":true}
```

```json
{"orderedImageIds":[101,102,103]}
```

#### `CartItemRequest` y `CartItemUpdateRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `CartItemRequest.variantId` | Long | Sí | `@NotNull`. |
| `CartItemRequest.quantity` | integer | Sí | `@NotNull`, mínimo 1, máximo 10000. |
| `CartItemUpdateRequest.quantity` | integer | Sí | `@NotNull`, mínimo 1, máximo 10000. |

```json
{"variantId":12,"quantity":2}
```

```json
{"quantity":3}
```

#### `CheckoutRequest` y `CheckoutItemRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `CheckoutRequest.customerName` | string | Sí | `@NotBlank`, máximo 255. |
| `customerEmail` | string | Sí | `@NotBlank`, `@Email`, máximo 255. |
| `customerPhone` | string | Sí | `@NotBlank`, máximo 50; no hay patrón telefónico en este DTO. |
| `shippingAddress` | string | Sí | `@NotBlank`; sin máximo declarado. |
| `customerNotes` | string | No | Sin restricciones declaradas. |
| `shippingCost` | decimal (`BigDecimal`) | No | Sin anotación de validación; el valor se recibe del request. |
| `paymentMethod` | `PaymentMethod` | Sí | `@NotNull`. |
| `customerTaxIdType` | `TaxIdType` | No | Sin anotación de presencia. |
| `customerTaxIdNumber` | string | No | Sin restricción declarada. |
| `items` | array de `CheckoutItemRequest` | No en Bean Validation | Cada ítem se valida en cascada con `@Valid`. Aunque el comentario del DTO plantea que puede omitirse para un usuario autenticado, el servicio exige una lista no vacía para cualquier checkout y no carga el carrito desde este método. |
| `CheckoutItemRequest.variantId` | Long | Sí | `@NotNull`. |
| `CheckoutItemRequest.quantity` | integer | Sí | `@NotNull`, `@Min(1)`. |

```json
{
  "customerName":"Ana Pérez",
  "customerEmail":"ana@example.com",
  "customerPhone":"1123456789",
  "shippingAddress":"Av. Ejemplo 123, CABA",
  "customerNotes":"Entregar por la tarde",
  "shippingCost":2500.00,
  "paymentMethod":"MERCADO_PAGO",
  "customerTaxIdType":"DNI",
  "customerTaxIdNumber":"30123456",
  "items":[{"variantId":12,"quantity":2}]
}
```

#### `SaleStatusUpdateRequest`, `PaymentStatusUpdateRequest`, `SaleInternalNoteRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `SaleStatusUpdateRequest.newStatus` | `SaleStatus` | Sí | `@NotNull`; transiciones se validan en negocio. |
| `SaleStatusUpdateRequest.changeReason` | string | No | Máximo 500. |
| `PaymentStatusUpdateRequest.newPaymentStatus` | `PaymentStatus` | Sí | `@NotNull`. |
| `PaymentStatusUpdateRequest.paymentNotes` | string | No | Máximo 500. |
| `SaleInternalNoteRequest.internalNotes` | string | Sí | `@NotBlank`, máximo 2000. |

```json
{"newStatus":"PREPARACION","changeReason":"Pago confirmado"}
```

```json
{"newPaymentStatus":"APROBADO","paymentNotes":"Transferencia conciliada"}
```

```json
{"internalNotes":"Preparar para despacho"}
```

#### `SaleReturnCreateRequest`, `SaleReturnItemRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `returnReason` | `ReturnReason` | Sí | `@NotNull`. |
| `returnResolution` | `ReturnResolution` | Sí | `@NotNull`. |
| `customerNotes` | string | Sí | `@NotBlank`, máximo 1000. |
| `items` | array de `SaleReturnItemRequest` | Sí | `@NotEmpty`; cascada `@Valid`. |
| `SaleReturnItemRequest.saleItemId` | Long | Sí | `@NotNull`. |
| `SaleReturnItemRequest.quantity` | integer | Sí | `@NotNull`, `@Min(1)`; límite de cantidad disponible validado en negocio. |

```json
{
  "returnReason":"CAMBIO_TALLE",
  "returnResolution":"CAMBIO_PRODUCTO",
  "customerNotes":"Solicito un talle mayor.",
  "items":[{"saleItemId":501,"quantity":1}]
}
```

#### `SaleReturnStatusUpdateRequest`, `SaleReturnInspectionRequest` y `SaleReturnRefundRequest`

| DTO / campo | Tipo | Requerido | Validaciones |
|---|---|---:|---|
| `SaleReturnStatusUpdateRequest.newStatus` | `ReturnStatus` | Sí | `@NotNull`; transiciones validadas por servicio. |
| `returnShippingGuide` | string | No | Máximo 100. |
| `statusNotes` | string | No | Máximo 500. |
| `SaleReturnInspectionRequest.inspectionNotes` | string | Sí | `@NotBlank`, máximo 1000. |
| `inspectionEvidencePath` | string | No | Máximo 500. |
| `inspectedItems` | array de `SaleReturnItemInspectionRequest` | Sí | `@NotEmpty`; cascada `@Valid`. |
| `SaleReturnItemInspectionRequest.returnItemId` | Long | Sí | `@NotNull`. |
| `restockApproved` | boolean | Sí | `@NotNull`. |
| `rejectionReason` | string | No | Máximo 255. |
| `SaleReturnRefundRequest.transactionReference` | string | No | Máximo 100. |
| `notes` | string | No | Máximo 500. |

```json
{"newStatus":"EN_TRANSITO","returnShippingGuide":"GUIA-123","statusNotes":"Retiro coordinado"}
```

```json
{
  "inspectionNotes":"Prenda sin uso.",
  "inspectionEvidencePath":"returns/expediente/fotos",
  "inspectedItems":[{"returnItemId":901,"restockApproved":true,"rejectionReason":null}]
}
```

```json
{"transactionReference":"REF-12345","notes":"Reintegro conciliado"}
```

### DTOs de response

Los campos de record se serializan con los nombres indicados. Las fechas `Instant` se representan en ISO-8601 UTC; `LocalDate` como `YYYY-MM-DD`; UUID como string. Un `Boolean` nullable puede resultar `null`; los campos primitivos `boolean` se serializan como boolean.

#### `AuthResponse` / `UserResponse`

`AuthResponse`: `accessToken` (string), `refreshToken` (string), `tokenType` (string, valor `Bearer`), `expiresAt` (Instant; la implementación lo calcula como vencimiento del refresh token) y `user` (`UserResponse`).

`UserResponse`: `userId` (UUID), `userRole` (`UserRole`), `userName`, `userEmail`, `userPhone`, `companyName` (string), `taxIdType` (`TaxIdType`), `taxIdNumber` (string), `lastLoginAt`, `passwordExpiresAt` (Instant). Los campos no marcados como requeridos en el DTO no tienen restricciones de presencia en la respuesta.

```json
{
  "accessToken":"<jwt>",
  "refreshToken":"<refresh-token>",
  "tokenType":"Bearer",
  "expiresAt":"2026-11-18T12:00:00Z",
  "user":{
    "userId":"550e8400-e29b-41d4-a716-446655440000",
    "userRole":"CUSTOMER",
    "userName":"Ana Pérez",
    "userEmail":"ana@example.com",
    "userPhone":"1123456789",
    "companyName":null,
    "taxIdType":"DNI",
    "taxIdNumber":"30123456",
    "lastLoginAt":"2026-11-11T12:00:00Z",
    "passwordExpiresAt":"2027-02-09T12:00:00Z"
  }
}
```

#### `ProductResponse`, `VariantResponse` y `ColorResponse`

- `ProductResponse`: `id` (UUID), `category` (`Category`), `name`, `shortDesc`, `longDesc` (string), `status` (`RowStatus`), `createdAt`, `updatedAt` (Instant).
- `VariantResponse`: `id` (Long), `productId` (UUID), `productName` (string), `colorId` (Long), `colorName`, `colorCode` (string), `size` (`NumericSize`), `targetGender` (`TargetGender`), `desc`, `sku` (string), `price` (BigDecimal), `stock` (Integer), `rowStatus` (`RowStatus`), `createdAt`, `updatedAt` (Instant).
- `ColorResponse`: `id` (Long), `name`, `desc`, `code` (string), `rowStatus` (`RowStatus`), `createdAt`, `updatedAt` (Instant).

```json
{
  "id":"550e8400-e29b-41d4-a716-446655440000",
  "category":"CALZADO",
  "name":"Zapatilla Urbana",
  "shortDesc":"Modelo liviano",
  "longDesc":"Descripción completa del producto.",
  "status":"ACTIVE",
  "createdAt":"2026-11-11T12:00:00Z",
  "updatedAt":"2026-11-11T12:00:00Z"
}
```

```json
{
  "id":12,
  "productId":"550e8400-e29b-41d4-a716-446655440000",
  "productName":"Zapatilla Urbana",
  "colorId":3,
  "colorName":"Negro",
  "colorCode":"#000000",
  "size":"T_38",
  "targetGender":"MUJERES",
  "desc":"Cuero negro",
  "sku":"URB-NEG-38",
  "price":45999.90,
  "stock":20,
  "rowStatus":"ACTIVE",
  "createdAt":"2026-11-11T12:00:00Z",
  "updatedAt":"2026-11-11T12:00:00Z"
}
```

```json
{"id":3,"name":"Negro","desc":"Negro clásico","code":"#000000","rowStatus":"ACTIVE","createdAt":"2026-11-11T12:00:00Z","updatedAt":"2026-11-11T12:00:00Z"}
```

#### `ImagePresignedUrlResponse` / `ImageResponse`

- `ImagePresignedUrlResponse`: `uploadUrl`, `publicUrl`, `storagePath` (string).
- `ImageResponse`: `imageId`, `variantId` (Long), `imageUrl` (string), `displayOrder` (Integer), `isPrimary` (boolean), `createdAt`, `updatedAt` (Instant).

```json
{"uploadUrl":"https://storage.example.com/signed-upload","publicUrl":"https://storage.example.com/public/products/image.webp","storagePath":"products/12/image.webp"}
```

```json
{"imageId":101,"variantId":12,"imageUrl":"https://storage.example.com/public/products/image.webp","displayOrder":0,"isPrimary":true,"createdAt":"2026-11-11T12:00:00Z","updatedAt":"2026-11-11T12:00:00Z"}
```

#### `CartResponse` / `CartItemResponse`

- `CartResponse`: `userId` (UUID), `items` (array), `totalUnits` (Integer), `grandTotal` (BigDecimal), `itemsPurged` y `itemsAdjusted` (boolean).
- `CartItemResponse`: `variantId` (Long), `productTitle`, `variantSku`, `attributesDescription`, `primaryImageUrl` (string), `unitPrice` (BigDecimal), `quantity` (Integer), `subtotal` (BigDecimal), `currentAvailableStock` (Integer), `wasAdjusted` (boolean).

```json
{
  "userId":"550e8400-e29b-41d4-a716-446655440000",
  "items":[{"variantId":12,"productTitle":"Zapatilla Urbana","variantSku":"URB-NEG-38","attributesDescription":"Negro, T_38","primaryImageUrl":"https://storage.example.com/image.webp","unitPrice":45999.90,"quantity":2,"subtotal":91999.80,"currentAvailableStock":20,"wasAdjusted":false}],
  "totalUnits":2,
  "grandTotal":91999.80,
  "itemsPurged":false,
  "itemsAdjusted":false
}
```

#### `SaleResponse`, `SaleSummaryResponse` y `SaleItemResponse`

- `SaleResponse`: `saleId`, `userId`, `trackingToken` (UUID); `customerName`, `customerEmail`, `customerPhone` (string); `customerTaxIdType` (`TaxIdType`), `customerTaxIdNumber`, `customerNotes`, `shippingAddress` (string); `saleQuantity` (Integer); `saleSubtotal`, `saleDiscountAmount`, `shippingCost`, `saleTotal` (BigDecimal); `saleShippedAt`, `saleDeliveredAt` (Instant); `saleStatus` (`SaleStatus`); `paymentMethod` (`PaymentMethod`); `paymentStatus` (`PaymentStatus`); `internalNotes`, `mpPreferenceId`, `mpInitPoint` (string); `createdAt`, `updatedAt` (Instant); `items` (array de `SaleItemResponse`).
- `SaleSummaryResponse`: `saleId`, `userId`, `trackingToken` (UUID); `customerName`, `customerEmail` (string); `customerTaxIdType` (`TaxIdType`), `customerTaxIdNumber` (string); `saleQuantity` (Integer), `saleTotal` (BigDecimal); `saleStatus` (`SaleStatus`), `paymentMethod` (`PaymentMethod`), `paymentStatus` (`PaymentStatus`), `createdAt` (Instant).
- `SaleItemResponse`: `saleItemId`, `variantId` (Long); `productName`, `productDesc`, `variantDesc`, `variantSku` (string); `variantSize` (`NumericSize`); `colorName`, `imageUrl` (string); `unitPrice`, `subtotal` (BigDecimal); `quantity` (Integer); `createdAt` (Instant).

```json
{
  "saleId":"550e8400-e29b-41d4-a716-446655440001",
  "userId":"550e8400-e29b-41d4-a716-446655440000",
  "trackingToken":"550e8400-e29b-41d4-a716-446655440002",
  "customerName":"Ana Pérez",
  "customerEmail":"ana@example.com",
  "customerPhone":"1123456789",
  "customerTaxIdType":"DNI",
  "customerTaxIdNumber":"30123456",
  "customerNotes":null,
  "shippingAddress":"Av. Ejemplo 123, CABA",
  "saleQuantity":2,
  "saleSubtotal":91999.80,
  "saleDiscountAmount":0.00,
  "shippingCost":2500.00,
  "saleTotal":94499.80,
  "saleShippedAt":null,
  "saleDeliveredAt":null,
  "saleStatus":"PENDIENTE",
  "paymentMethod":"MERCADO_PAGO",
  "paymentStatus":"PENDIENTE",
  "internalNotes":null,
  "mpPreferenceId":"<preference-id>",
  "mpInitPoint":"https://www.mercadopago.com/checkout/v1/redirect?pref_id=<id>",
  "createdAt":"2026-11-11T12:00:00Z",
  "updatedAt":"2026-11-11T12:00:00Z",
  "items":[{"saleItemId":501,"variantId":12,"productName":"Zapatilla Urbana","productDesc":"Modelo liviano","variantDesc":"Cuero negro","variantSku":"URB-NEG-38","variantSize":"T_38","colorName":"Negro","imageUrl":"https://storage.example.com/image.webp","unitPrice":45999.90,"quantity":2,"subtotal":91999.80,"createdAt":"2026-11-11T12:00:00Z"}]
}
```

#### `SaleReturnResponse`, `SaleReturnSummaryResponse` y `SaleReturnItemResponse`

- `SaleReturnResponse`: `returnId`, `saleId`, `trackingToken` (UUID); `returnReason` (`ReturnReason`), `returnResolution` (`ReturnResolution`), `returnStatus` (`ReturnStatus`); `shippingCoveredByStore` (Boolean); `returnShippingGuide` (string); `refundAmount` (BigDecimal); `customerNotes`, `inspectionNotes`, `inspectionEvidencePath` (string); `inspectedBy` (UUID); `inspectedAt`, `createdAt`, `updatedAt`, `closedAt` (Instant); `items` (array).
- `SaleReturnSummaryResponse`: `returnId`, `saleId`, `trackingToken` (UUID); `customerName`, `customerEmail` (string); `returnReason` (`ReturnReason`), `returnResolution` (`ReturnResolution`), `returnStatus` (`ReturnStatus`); `refundAmount` (BigDecimal); `createdAt`, `closedAt` (Instant).
- `SaleReturnItemResponse`: `returnItemId`, `saleItemId`, `variantId` (Long); `productName`, `variantDesc`, `variantSku` (string); `variantSize` (`NumericSize`); `colorName`, `imageUrl` (string); `unitPrice`, `subtotal` (BigDecimal); `quantity` (Integer); `restockApproved` (Boolean); `rejectionReason` (string); `createdAt` (Instant).

```json
{
  "returnId":"550e8400-e29b-41d4-a716-446655440010",
  "saleId":"550e8400-e29b-41d4-a716-446655440001",
  "trackingToken":"550e8400-e29b-41d4-a716-446655440011",
  "returnReason":"CAMBIO_TALLE",
  "returnResolution":"CAMBIO_PRODUCTO",
  "returnStatus":"SOLICITADA",
  "shippingCoveredByStore":null,
  "returnShippingGuide":null,
  "refundAmount":null,
  "customerNotes":"Solicito un talle mayor.",
  "inspectionNotes":null,
  "inspectionEvidencePath":null,
  "inspectedBy":null,
  "inspectedAt":null,
  "createdAt":"2026-11-11T12:00:00Z",
  "updatedAt":"2026-11-11T12:00:00Z",
  "closedAt":null,
  "items":[{"returnItemId":901,"saleItemId":501,"variantId":12,"productName":"Zapatilla Urbana","variantDesc":"Cuero negro","variantSku":"URB-NEG-38","variantSize":"T_38","colorName":"Negro","imageUrl":"https://storage.example.com/image.webp","unitPrice":45999.90,"quantity":1,"subtotal":45999.90,"restockApproved":null,"rejectionReason":null,"createdAt":"2026-11-11T12:00:00Z"}]
}
```

#### `FiscalReceiptResponse`

Campos: `fiscalReceiptId`, `saleId`, `returnId` (UUID); `receiptType` (`FiscalReceiptType`); `pointOfSale` (Integer); `receiptNumber` (Long); `formattedReceiptNumber`, `cae` (string); `caeDueDate`, `issueDate` (LocalDate); `docType` (Integer); `docNumber`, `customerName` (string); `totalAmount` (BigDecimal); `qrPayload` (string); `createdAt` (Instant).

```json
{
  "fiscalReceiptId":"550e8400-e29b-41d4-a716-446655440020",
  "saleId":"550e8400-e29b-41d4-a716-446655440001",
  "returnId":null,
  "receiptType":"FACTURA_C",
  "pointOfSale":1,
  "receiptNumber":123,
  "formattedReceiptNumber":"00001-00000123",
  "cae":"<cae>",
  "caeDueDate":"2026-11-21",
  "issueDate":"2026-11-11",
  "docType":96,
  "docNumber":"30123456",
  "customerName":"Ana Pérez",
  "totalAmount":94499.80,
  "qrPayload":"<payload-qr>",
  "createdAt":"2026-11-11T12:00:00Z"
}
```

## 6. Observaciones de integración y atención requerida

- **Autorización antes de producción:** configurar cadena/filtro JWT y autorización por rol; proteger todos los `/admin/...`, escrituras de variantes/colores/imágenes y rutas de usuario. Asegurar propiedad del recurso en `UserController`.
- **Validación faltante:** `POST /auth/logout` deserializa `RefreshTokenRequest` sin `@Valid`; `POST /payments/mercadopago/webhook` recibe mapa opcional sin validación de esquema (la firma se valida en el servicio). El resto de bodies tipados observados usa `@Valid`; `items` anidados llevan validación en cascada.
- **Contrato de checkout:** `shippingCost` llega desde el cliente sin restricción de validación; confirmarlo como decisión comercial antes de confiar en su valor.
- **Checkout y DTO:** el comentario de `CheckoutRequest` describe `items` como opcional para usuario autenticado con carrito, pero `SaleService.createSale` rechaza la lista ausente/vacía antes de consultar al usuario y procesa los ítems enviados; alinear la documentación y el flujo de carrito.
- **Eliminación de imágenes:** el servicio captura fallos del borrado remoto de Storage y no los devuelve al cliente; `204` solo confirma el borrado en la base de datos, no necesariamente el archivo.
- **Serialización de página:** el endpoint devuelve `Page` de Spring Data, no un DTO propio; campos auxiliares pueden variar con la versión de Spring Data/Jackson.
- **Prefijo `/api`:** está escrito como propiedad, pero los mappings observados son absolutos desde la raíz y no se encontró configuración que aplique el prefijo.
- **Códigos de error:** no hay respuesta dedicada `503`; ARCA no cuenta con handler específico y puede producir `500`. El handler genérico revela actualmente el mensaje de excepción.
- **Enums de rol:** unificar `SUPER_ADMIN` con los nombres mencionados en las anotaciones comentadas antes de activar `@PreAuthorize`.
- **DTOs:** `AuthResponse.expiresAt` se construye con el vencimiento del refresh token (7 días), no con el TTL del access token (15 minutos); el cliente debe leer el vencimiento JWT del propio access token o evitar interpretar `expiresAt` como su expiración.
- **Docker:** falta Dockerfile/Compose y la base de datos no se inicializa automáticamente; el esquema debe estar creado previamente.
