# Proyecto TechLab — API REST Spring Boot

API REST de e-commerce y gestión integral para catálogo con variantes, carrito, checkout multicanal, Mercado Pago, envíos por zona, facturación electrónica ARCA y posventa/devoluciones.

## Arquitectura y decisión de empaquetado

El proyecto usa **Package by Feature**: los módulos encapsulan de forma vertical cada dominio funcional y sus capas internas.

La arquitectura original estaba proyectada como una organización horizontal clásica (`controller/`, `service/`, `repository/`, `model/`). El crecimiento del dominio superó los 11 módulos funcionales independientes —`auth`, `user`, `product`, `variant`, `color`, `image`, `cart`, `shipping`, `sale`, `salereturn` y `fiscal`— y motivó migrar hacia una arquitectura vertical orientada a features.

Cada feature contiene, según corresponda, `controller`, `service`, `mapper`, `dto`, `model`, `repository` y `exception`. `common` concentra excepciones transversales, DTO de error, estado de filas y utilidades. La delegación entre servicios se mantiene dentro del dominio y se evitan consultas N+1.

```text
Cliente REST / React
        |
        v
  Controller
        |
        v
  Service
        |
        v
Domain Service -> Mapper -> Client
        |                         |
        v                         v
   Repository --------> Mercado Pago / ARCA / Storage
        |
        v
Supabase PostgreSQL
```

## Repositorios relacionados

| Proyecto | Repositorio |
|---|---|
| Frontend React + TypeScript + Vite | https://github.com/TU_USUARIO/e_commerce_front |
| Base de datos PostgreSQL / Supabase DDL | https://github.com/TU_USUARIO/e_commerce_db |
| Backend Spring Boot | https://github.com/TU_USUARIO/e_commerce_back |

> Los enlaces son placeholders listos para reemplazar.

## Stack tecnológico

| Tecnología | Versión / detalle |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Maven Wrapper | 3.3.4; Apache Maven 3.9.16 |
| Spring Web MVC | `spring-boot-starter-webmvc`, versión heredada del BOM Spring Boot |
| Spring Security | `spring-boot-starter-security`, versión heredada del BOM Spring Boot |
| Spring Data JPA / Hibernate | `spring-boot-starter-data-jpa`, versión heredada del BOM Spring Boot |
| Bean Validation | `spring-boot-starter-validation`, versión heredada del BOM Spring Boot |
| PostgreSQL JDBC | artifact `org.postgresql:postgresql`, versión heredada del BOM |
| Lombok | versión heredada del BOM Spring Boot |
| JJWT | 0.12.6: `jjwt-api`, `jjwt-impl`, `jjwt-jackson` |
| Mercado Pago Java SDK | 3.7.0 |
| BouncyCastle | 1.78.1: `bcprov-jdk18on`, `bcpkix-jdk18on` |
| ARCA / SOAP | Cliente nativo `java.net.http.HttpClient` y XML DOM; no Apache CXF |

No existen Springdoc/OpenAPI, Testcontainers, Spring Actuator activo ni Apache CXF como dependencias del POM.

## Instalación y ejecución

### Prerrequisitos

- JDK 21 instalado y configurado como `JAVA_HOME`.
- Git.
- Conexión a internet.
- IDE IntelliJ IDEA o VS Code.

### Base de datos Supabase

La base PostgreSQL está alojada remotamente en **Supabase** y ya contiene datos iniciales de prueba. No es necesario instalar PostgreSQL local ni ejecutar scripts DDL manualmente.

Hibernate está configurado con `spring.jpa.hibernate.ddl-auto=validate`, por lo que valida el modelo sin crear ni modificar tablas.

### Archivo `.env`

Al clonar el proyecto, ubique el archivo `.env` en la raíz de `e_commerce_back`. Puede obtenerse junto a la entrega o tomarse `.env.example` como referencia.

El perfil activo por defecto es `dev`. En ese perfil existen clientes simulados:

- `MockMercadoPagoClient` para pagos.
- `MockWsaaTokenService` y `MockWsfeSoapService` para autenticación y facturación ARCA.

Por lo tanto, el perfil `dev` permite probar pagos y facturación sin certificados reales ni credenciales productivas ARCA.

Variables declaradas por `application.properties`:

| Grupo | Variables |
|---|---|
| Base de datos | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_SHOW_SQL`, `SPRING_JPA_FORMAT_SQL` |
| JWT | `SPRING_SECRET_SHA256_KEY`, `SPRING_JWT_EXPIRATION`, `SPRING_INITIAL_USER_PASSWORD` |
| CORS | `CORS_ALLOWED_ORIGINS` |
| Supabase Storage | `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `SUPABASE_STORAGE_BUCKET` |
| Mercado Pago | `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET`, `MP_NOTIFICATION_URL`, `MP_BACK_URL_SUCCESS`, `MP_BACK_URL_FAILURE`, `MP_BACK_URL_PENDING`, `MP_SANDBOX_URL` |
| Ventas vencidas | `SALES_EXPIRATION_CRON`, `SALES_EXPIRATION_ZONE`, `SALES_EXPIRATION_TTL_HOURS`, `SALES_EXPIRATION_CASH_TTL_HOURS` |
| ARCA | `ARCA_CUIT`, `ARCA_PUNTO_VENTA`, `ARCA_CERT_PATH`, `ARCA_KEY_PATH`, `ARCA_WSAA_URL`, `ARCA_WSFE_URL`, `ARCA_ANONYMOUS_INVOICE_LIMIT`, `ARCA_RETRY_CRON`, `ARCA_RETRY_ZONE` |

El proyecto no declara `spring.config.import` en `application.properties` ni incluye una dependencia dotenv explícita en el POM. Por ello, las variables deben exportarse al proceso de la terminal o cargarse mediante el plugin/configuración EnvFile de su IDE antes de ejecutar.

Linux/macOS — desde la raíz de `proyecto_techlab_springboot`:

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

Windows PowerShell — desde la raíz de `proyecto_techlab_springboot`:

```powershell
Get-Content .env | ForEach-Object { if ($_ -match '^([^#][^=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process') } }
.\mvnw.cmd spring-boot:run
```

### Terminal Linux/macOS

```bash
cd proyecto_techlab_springboot
chmod +x mvnw
./mvnw clean spring-boot:run
```

### Terminal Windows

PowerShell o CMD:

```powershell
cd proyecto_techlab_springboot
.\mvnw.cmd clean spring-boot:run
```

Git Bash:

```bash
cd proyecto_techlab_springboot
./mvnw.cmd clean spring-boot:run
```

### IntelliJ IDEA

1. Abra el `pom.xml` como proyecto Maven.
2. Configure Project SDK = Java 21.
3. Habilite **Annotation Processing** para Lombok.
4. Configure las variables del `.env` mediante variables de entorno o el plugin EnvFile.
5. Ejecute `ProyectoTechlabSpringbootApplication`.

### Visual Studio Code

1. Instale **Extension Pack for Java** y **Spring Boot Extension Pack**.
2. Instale **REST Client** (`humao.rest-client`) si va a usar `api-requests.http`.
3. Abra la carpeta `proyecto_techlab_springboot`.
4. Lance la aplicación desde **Spring Boot Dashboard**.

El servidor no declara `server.port` activo; Spring Boot usa su puerto predeterminado.

### Formas de probar

- Levantar `e_commerce_front` para operar visualmente.
- Ejecutar `api-requests.http` con VS Code REST Client.
- Importar las rutas en Postman o invocarlas con cURL.

## Prefijo API y paginación

`ApiWebConfiguration` agrega `/api` a todos los `@RestController`. Ejemplo: `@RequestMapping("/auth")` se expone como `/api/auth`.

Los listados paginados aceptan:

```text
?page=0&size=20&sort=campo,asc
```

El formato de respuesta se configura con `PageSerializationMode.VIA_DTO`:

```json
{
  "content": [],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

Defaults reales: productos `20`, variantes `20`, ventas propias `15`, ventas administrativas `20`, devoluciones propias `10`, devoluciones administrativas `15`.

## Seguridad JWT y usuarios de prueba

### Flujo

1. Ejecutar `POST /api/auth/login` con `email` y `password`. Este endpoint exige que el cliente sea anónimo.
2. Recibir `200 OK` con `AuthResponse`: `accessToken`, `refreshToken`, `tokenType: "Bearer"`, `expiresAt` y `user`.
3. Enviar el access token:

```http
Authorization: Bearer <accessToken>
```

4. Renovar con `POST /api/auth/refresh` y cerrar sesión con `POST /api/auth/logout`.

El access token usa los claims `userId`, `role`, `sub` (email), `iat` y `exp`. Su duración se obtiene de `SPRING_JWT_EXPIRATION`.

El refresh token se almacena como hash en `UserSession`. `refresh` devuelve un nuevo par de tokens. `logout` acepta un `RefreshTokenRequest` opcional y revoca la sesión incluida.

Roles reales del enum `UserRole`:

- `CUSTOMER`
- `WHOLESALER`
- `OPERATOR`
- `ADMIN`

### Usuarios precargados

| Rol | Email | Contraseña | Alcance principal |
|---|---|---|---|
| `ADMIN` | `admin_1@gmail.com` | `admin_1` | Control total, operadores, ventas, fiscal, devoluciones y catálogo |
| `OPERATOR` | `operator_1@gmail.com` | `operator_1` | Catálogo, pedidos, pagos, envíos y devoluciones |
| `WHOLESALER` | `wholesaler_1@gmail.com` | `wholesaler_1` | Descuento mayorista y mínimo de 10 unidades |
| `CUSTOMER` | `customer_1@gmail.com` | `customer_1` | Compras minoristas, carrito y devoluciones |

## Reglas de acceso

Anónimos:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/change-expired-password`

Públicos:

- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/dev/**`
- `GET /api/products/**`
- `GET /api/variants/**`
- `GET /api/colors/**`
- `GET /api/shipping/**`
- `POST /api/cart/sync`
- `POST /api/sales/checkout`
- `POST /api/payments/mercadopago/webhook`
- `POST /api/returns/guest/**`
- `GET /api/sales/track/**`
- `GET /api/returns/track/**`

Rutas administrativas usan `OPERATOR` o `ADMIN`; alta de operadores usa `ADMIN`; ciertas rutas de usuario permiten al propio usuario o a `OPERATOR`/`ADMIN`.

## Flujos funcionales

### Catálogo

Orden obligatorio:

1. `POST /api/admin/colors`.
2. `POST /api/admin/products`.
3. `POST /api/variants` con `productId` y `colorId`.
4. `POST /api/products/{productId}/colors/{colorId}/images`.

Una variante debe referenciar producto y color válidos, usar talle, SKU, precio y stock coherentes.

### Carrito

- Autenticado: `GET/POST/PATCH/DELETE /api/cart` persiste `CartItem` por usuario.
- Invitado: `POST /api/cart/sync` consolida cantidades, purga líneas inválidas o ajusta cantidades según stock.
- `CartResponse` informa `itemsPurged`, `itemsAdjusted`, `totalUnits` y `grandTotal`.
- Cantidad mínima `1`; máximo `10000`.
- Checkout autenticado puede usar el carrito persistente; checkout invitado requiere items.

### Checkout y descuentos

`POST /api/sales/checkout` acepta invitados o usuarios autenticados.

Campos:

```json
{
  "customerName": "string",
  "customerEmail": "string",
  "customerPhone": "string",
  "shippingAddress": "string",
  "customerNotes": "string",
  "isPickup": true,
  "shippingProvince": "string",
  "shippingLocality": "string",
  "shippingPostalCode": "string",
  "paymentMethod": "MERCADO_PAGO",
  "customerTaxIdType": "CUIT",
  "customerTaxIdNumber": "string",
  "items": [
    {
      "variantId": 1,
      "quantity": 1
    }
  ]
}
```

Descuentos calculados por `SalePolicyService`:

| Rol | Regla |
|---|---|
| Invitado / `CUSTOMER` | 5% desde 3 unidades; antes, 0% |
| `WHOLESALER` | 10%; mínimo 10 unidades |
| `OPERATOR` | 10%, sin mínimo |
| `ADMIN` | 15%, sin mínimo |

### Envíos

Métodos reales `ShippingMethod`:

- `RETIRO_LLAVALLOL`
- `ENVIO_GBA_ZONA_SUR`
- `ENVIO_GBA_GENERAL`
- `ENVIO_CORREO_NACIONAL`

Cotización pública:

```text
GET /api/shipping/quote?isPickup=true&province=&locality=&postalCode=
```

### Métodos de pago

El enum `PaymentMethod` contiene:

- `MERCADO_PAGO`
- `TRANSFERENCIA`
- `EFECTIVO`

Mercado Pago genera `mpPreferenceId` y `mpInitPoint`. En `dev`, `POST /api/dev/payments/mercadopago/simulate` acepta `approved`, `pending` o `rejected`.

Transferencia y efectivo quedan sujetos a conciliación operativa. Las ventas con envío/preparación que no sean efectivo requieren pago aprobado.

### Estados de ventas

Estados `SaleStatus`:

`PENDIENTE`, `PREPARACION`, `ENVIADO`, `ENTREGADO`, `CANCELADO`, `REEMBOLSADO`, `AUDITORIA`, `RECLAMO_ABIERTO`, `CERRADO_CON_DEVOLUCION`.

Matriz administrativa:

```text
PENDIENTE -> PREPARACION | CANCELADO | AUDITORIA
AUDITORIA -> PREPARACION | CANCELADO
PREPARACION -> ENVIADO | CANCELADO
ENVIADO -> ENTREGADO | CANCELADO
ENTREGADO -> REEMBOLSADO
CANCELADO -> terminal
REEMBOLSADO -> terminal
```

Estados `PaymentStatus`:

`PENDIENTE`, `APROBADO`, `RECHAZADO`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO`.

Las notas internas se agregan con `SaleInternalNoteFormatter.prepend`: nueva entrada primero, con actor y timestamp en zona `America/Argentina/Buenos_Aires`.

### Posventa y devoluciones

Condiciones:

- Venta en estado `ENTREGADO`.
- Dentro de 10 días desde `saleDeliveredAt`.
- Sin devolución activa duplicada.
- Cantidades solicitadas no superiores a las compradas.

Motivos: `ARREPENTIMIENTO_LEGAL`, `CAMBIO_TALLE`, `DEFECTO_FABRICACION`.

Resoluciones: `REEMBOLSO_DINERO`, `CAMBIO_PRODUCTO`, `CREDITO_TIENDA`.

Estados y transiciones:

```text
SOLICITADA -> EN_TRANSITO | RECIBIDO_EN_CONTROL | CANCELADA
EN_TRANSITO -> RECIBIDO_EN_CONTROL | CANCELADA
RECIBIDO_EN_CONTROL -> APROBADA | RECHAZADA
APROBADA, RECHAZADA, CANCELADA -> terminal
```

El administrador inspecciona por ítem mediante `/inspect`, decide `restockApproved`, procesa reembolso y registra conciliación.

### Facturación ARCA

Tipos fiscales reales:

- `FACTURA_C` — código AFIP `11`.
- `NOTA_DEBITO_C` — código AFIP `12`.
- `NOTA_CREDITO_C` — código AFIP `13`.

La aplicación usa WSAA/WSFE mediante HTTP SOAP nativo y BouncyCastle. En perfil `dev` se usan los mocks fiscales.

## Catálogo de endpoints

Todas las rutas requieren agregar `/api` al prefijo anotado por el controlador.

### Auth

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| POST | `/api/auth/login` | Anónimo | `LoginRequest(email,password)` | `AuthResponse`, 200 |
| POST | `/api/auth/change-expired-password` | Anónimo | `ExpiredPasswordResetRequest` | `AuthResponse`, 200 |
| POST | `/api/auth/refresh` | Público | `RefreshTokenRequest(refreshToken)` | `AuthResponse`, 200 |
| POST | `/api/auth/register` | Anónimo | `UserCreateRequest` | `UserResponse`, 201 |
| POST | `/api/auth/register/wholesalers` | `OPERATOR`/`ADMIN` | `WholesalerCreateRequest` | `UserResponse`, 201 |
| POST | `/api/auth/register/operators` | `ADMIN` | `OperatorCreateRequest` | `UserResponse`, 201 |
| GET | `/api/auth/me` | Autenticado | JWT | `UserResponse`, 200 |
| POST | `/api/auth/logout` | Público | body opcional `RefreshTokenRequest` | vacío, 204 |

### Usuarios

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/users/{id}` | Propio usuario/`OPERATOR`/`ADMIN` | UUID `id` | `UserResponse`, 200 |
| PUT | `/api/users/{id}` | Propio usuario/`OPERATOR`/`ADMIN` | `UserUpdateRequest` | `UserResponse`, 200 |
| PATCH | `/api/users/{id}/password` | Propio usuario/`OPERATOR`/`ADMIN` | `PasswordUpdateRequest` | `UserResponse`, 200 |
| PATCH | `/api/users/{id}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `UserResponse`, 200 |
| GET | `/api/admin/users` | `OPERATOR`/`ADMIN` | `segment`, `roleFilter`, `rowStatus`, `search`, Pageable 20 | `Page<UserResponse>`, 200 |
| PUT | `/api/admin/users/{id}` | `OPERATOR`/`ADMIN` | `AdminUserUpdateRequest` | `UserResponse`, 200 |
| PATCH | `/api/admin/users/{id}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `UserResponse`, 200 |

### Productos

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/products?targetGender=&page=&size=&sort=` | Público | `TargetGender` obligatorio; Pageable 20 | `Page<ProductCatalogResponse>`, 200 |
| GET | `/api/products/{id}` | Público | UUID `id` | `ProductResponse`, 200 |
| POST | `/api/admin/products` | `OPERATOR`/`ADMIN` | `ProductCreateRequest` | `ProductResponse`, 201 |
| GET | `/api/admin/products?targetGender=&rowStatus=&search=&page=&size=&sort=` | `OPERATOR`/`ADMIN` | filtros; Pageable 20 | `Page<ProductResponse>`, 200 |
| GET | `/api/admin/products/options` | `OPERATOR`/`ADMIN` | ninguno | `List<ProductOptionResponse>`, 200 |
| GET | `/api/admin/products/{id}` | `OPERATOR`/`ADMIN` | UUID `id` | `ProductResponse`, 200 |
| PUT | `/api/admin/products/{id}` | `OPERATOR`/`ADMIN` | `ProductUpdateRequest` | `ProductResponse`, 200 |
| PATCH | `/api/admin/products/{id}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `ProductResponse`, 200 |

### Colores y variantes

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/colors?includeInactive=false` | Público | boolean opcional | `List<ColorResponse>`, 200 |
| GET | `/api/colors/{id}` | Público | Long `id` | `ColorResponse`, 200 |
| POST | `/api/admin/colors` | `OPERATOR`/`ADMIN` | `ColorCreateRequest` | `ColorResponse`, 201 |
| PUT | `/api/admin/colors/{id}` | `OPERATOR`/`ADMIN` | `ColorUpdateRequest` | `ColorResponse`, 200 |
| PATCH | `/api/admin/colors/{id}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `ColorResponse`, 200 |
| GET | `/api/variants` | `OPERATOR`/`ADMIN` | filtros; Pageable 20 | `Page<VariantResponse>`, 200 |
| GET | `/api/variants/by-product/{productId}?includeInactive=false` | Público | UUID, boolean | `List<VariantResponse>`, 200 |
| GET | `/api/variants/public/by-product/{productId}` | Público | UUID | `List<PublicProductVariantResponse>`, 200 |
| GET | `/api/variants/{id}` | Público | Long `id` | `VariantResponse`, 200 |
| POST | `/api/variants` | `OPERATOR`/`ADMIN` | `VariantCreateRequest` | `VariantResponse`, 201 |
| PUT | `/api/variants/{id}` | `OPERATOR`/`ADMIN` | `VariantUpdateRequest` | `VariantResponse`, 200 |
| PATCH | `/api/variants/{id}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `VariantResponse`, 200 |

### Imágenes

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| POST | `/api/products/{productId}/colors/{colorId}/images/presigned-url` | `OPERATOR`/`ADMIN` | `ImagePresignedUrlRequest` | `ImagePresignedUrlResponse`, 200 |
| POST | `/api/products/{productId}/colors/{colorId}/images` | `OPERATOR`/`ADMIN` | `ImageCreateRequest` | `ImageResponse`, 201 |
| GET | `/api/products/{productId}/colors/{colorId}/images` | `OPERATOR`/`ADMIN` | ninguno | `List<ImageResponse>`, 200 |
| PATCH | `/api/products/{productId}/colors/{colorId}/images/{imageId}/primary` | `OPERATOR`/`ADMIN` | path IDs | `ImageResponse`, 200 |
| DELETE | `/api/products/{productId}/colors/{colorId}/images/{imageId}` | `OPERATOR`/`ADMIN` | path IDs | vacío, 204 |

### Carrito

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/cart` | Autenticado | JWT | `CartResponse`, 200 |
| POST | `/api/cart/item` | Autenticado | `CartItemRequest` | `CartResponse`, 201 |
| POST | `/api/cart/sync` | Público | `CartSyncRequest` | `CartResponse`, 200 |
| PATCH | `/api/cart/item/{variantId}` | Autenticado | `CartItemUpdateRequest` | `CartResponse`, 200 |
| DELETE | `/api/cart/item/{variantId}` | Autenticado | Long `variantId` | vacío, 204 |
| DELETE | `/api/cart` | Autenticado | ninguno | vacío, 204 |

### Envíos

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/shipping/options` | Público | ninguno | `List<ShippingZoneResponse>`, 200 |
| GET | `/api/shipping/quote` | Público | `isPickup`, `province`, `locality`, `postalCode` | `ShippingQuoteResponse`, 200 |
| GET | `/api/admin/shipping` | `OPERATOR`/`ADMIN` | ninguno | `List<ShippingZoneResponse>`, 200 |
| GET | `/api/admin/shipping/{shippingZoneId}` | `OPERATOR`/`ADMIN` | Long ID | `ShippingZoneResponse`, 200 |
| PUT | `/api/admin/shipping/{shippingZoneId}` | `OPERATOR`/`ADMIN` | `ShippingZoneUpdateRequest` | `ShippingZoneResponse`, 200 |
| PATCH | `/api/admin/shipping/{shippingZoneId}/status` | `OPERATOR`/`ADMIN` | `StatusUpdateRequest` | `ShippingZoneResponse`, 200 |

### Ventas y pagos

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| POST | `/api/sales/checkout` | Invitado/autenticado | `CheckoutRequest` | `SaleResponse`, 201 |
| POST | `/api/sales/me/{saleId}/retry-mp` | Autenticado | UUID `saleId` | `SaleResponse`, 200 |
| GET | `/api/sales/track/{trackingToken}` | Público | UUID tracking token | `SaleResponse`, 200 |
| GET | `/api/sales/me` | Autenticado | `statuses`, Pageable 15 | `Page<SaleSummaryResponse>`, 200 |
| GET | `/api/sales/me/{saleId}` | Autenticado | UUID `saleId` | `SaleResponse`, 200 |
| PATCH | `/api/sales/me/{saleId}/cancel` | Autenticado | UUID `saleId` | `SaleResponse`, 200 |
| GET | `/api/admin/sales` | `OPERATOR`/`ADMIN` | filtros + Pageable 20 | `Page<SaleSummaryResponse>`, 200 |
| GET | `/api/admin/sales/{saleId}` | `OPERATOR`/`ADMIN` | UUID `saleId` | `SaleResponse`, 200 |
| PATCH | `/api/admin/sales/{saleId}/status` | `OPERATOR`/`ADMIN` | `SaleStatusUpdateRequest` | `SaleResponse`, 200 |
| PATCH | `/api/admin/sales/{saleId}/payment-status` | `OPERATOR`/`ADMIN` | `PaymentStatusUpdateRequest` | `SaleResponse`, 200 |
| PATCH | `/api/admin/sales/{saleId}/internal-notes` | `OPERATOR`/`ADMIN` | `SaleInternalNoteRequest` | `SaleResponse`, 200 |
| PATCH | `/api/admin/sales/{saleId}/fiscal-data` | `OPERATOR`/`ADMIN` | `SaleFiscalDataUpdateRequest` | `SaleResponse`, 200 |
| POST | `/api/payments/mercadopago/webhook` | Público | `x-signature`, `topic`, `id`, body opcional | vacío, 200 |
| POST | `/api/dev/payments/mercadopago/simulate` | Público, perfil dev | `DevMercadoPagoSimulateRequest` | `DevMercadoPagoSimulateResponse`, 200 |

### Devoluciones

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| POST | `/api/returns/orders/{saleId}` | Autenticado | `SaleReturnCreateRequest` | `SaleReturnResponse`, 201 |
| POST | `/api/returns/guest/{trackingToken}` | Público | `SaleReturnCreateRequest` | `SaleReturnResponse`, 201 |
| GET | `/api/returns/track/{returnTrackingToken}` | Público | UUID | `SaleReturnResponse`, 200 |
| GET | `/api/returns/me` | Autenticado | Pageable 10 | `Page<SaleReturnSummaryResponse>`, 200 |
| GET | `/api/returns/orders/{saleId}` | Autenticado | UUID | `List<SaleReturnResponse>`, 200 |
| GET | `/api/admin/returns` | `OPERATOR`/`ADMIN` | filtros + Pageable 15 | `Page<SaleReturnSummaryResponse>`, 200 |
| GET | `/api/admin/returns/sales/{saleId}` | `OPERATOR`/`ADMIN` | UUID | `List<SaleReturnResponse>`, 200 |
| GET | `/api/admin/returns/{returnId}` | `OPERATOR`/`ADMIN` | UUID | `SaleReturnResponse`, 200 |
| PATCH | `/api/admin/returns/{returnId}/status` | `OPERATOR`/`ADMIN` | `SaleReturnStatusUpdateRequest` | `SaleReturnResponse`, 200 |
| POST | `/api/admin/returns/{returnId}/inspect` | `OPERATOR`/`ADMIN` | `SaleReturnInspectionRequest` | `SaleReturnResponse`, 200 |
| POST | `/api/admin/returns/{returnId}/process-refund` | `OPERATOR`/`ADMIN` | `SaleReturnRefundRequest` opcional | `SaleReturnResponse`, 200 |

### Facturación ARCA

| Método | Ruta | Acceso | Request/params | Respuesta |
|---|---|---|---|---|
| GET | `/api/admin/fiscal/sales/{saleId}` | `OPERATOR`/`ADMIN` | UUID `saleId` | `List<FiscalReceiptResponse>`, 200 |
| GET | `/api/admin/fiscal/{fiscalReceiptId}` | `OPERATOR`/`ADMIN` | UUID `fiscalReceiptId` | `FiscalReceiptResponse`, 200 |
| POST | `/api/admin/fiscal/sales/{saleId}/retry-invoice` | `OPERATOR`/`ADMIN` | UUID `saleId`, JWT | `FiscalReceiptResponse`, 200 |

## DTOs de entrada relevantes

- `LoginRequest`: `email`, `password`.
- `RefreshTokenRequest`: `refreshToken`.
- `ExpiredPasswordResetRequest`: `email`, `currentPassword`, `newPassword`.
- `UserCreateRequest`: `name`, `email`, `password`, `phone`, `taxIdType`, `taxIdNumber`.
- `WholesalerCreateRequest`: `name`, `email`, `password`, `phone`, `companyName`, `taxIdType`, `taxIdNumber`.
- `OperatorCreateRequest`: `name`, `email`, `phone`.
- `ColorCreateRequest/UpdateRequest`: `name`, `desc`, `code`.
- `ProductCreateRequest/UpdateRequest`: `targetGender`, `name`, `shortDesc`, `longDesc`.
- `VariantCreateRequest`: `productId`, `colorId`, `size`, `desc`, `sku`, `price`, `stock`.
- `VariantUpdateRequest`: `colorId`, `size`, `desc`, `sku`, `price`, `stock`.
- `ImagePresignedUrlRequest`: `originalFileName`, `contentType`.
- `ImageCreateRequest`: `imageUrl`, `isPrimary`.
- `CartItemRequest`: `variantId`, `quantity`.
- `CartItemUpdateRequest`: `quantity`.
- `CartSyncRequest`: `items`.
- `CheckoutRequest`: `customerName`, `customerEmail`, `customerPhone`, `shippingAddress`, `customerNotes`, `isPickup`, `shippingProvince`, `shippingLocality`, `shippingPostalCode`, `paymentMethod`, `customerTaxIdType`, `customerTaxIdNumber`, `items`.
- `CheckoutItemRequest`: `variantId`, `quantity`.
- `SaleStatusUpdateRequest`: `newStatus`, `changeReason`.
- `PaymentStatusUpdateRequest`: `newPaymentStatus`, `paymentNotes`.
- `SaleInternalNoteRequest`: `internalNotes`.
- `SaleFiscalDataUpdateRequest`: `customerName`, `customerTaxIdType`, `customerTaxIdNumber`.
- `SaleReturnCreateRequest`: `returnReason`, `returnResolution`, `customerNotes`, `items`.
- `SaleReturnItemRequest`: `saleItemId`, `quantity`.
- `SaleReturnStatusUpdateRequest`: `newStatus`, `returnShippingGuide`, `statusNotes`.
- `SaleReturnInspectionRequest`: `inspectionNotes`, `inspectionEvidencePath`, `inspectedItems`.
- `SaleReturnItemInspectionRequest`: `returnItemId`, `restockApproved`, `rejectionReason`.
- `SaleReturnRefundRequest`: `transactionReference`, `notes`.

## Ejemplos JSON

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin_1@gmail.com",
  "password": "admin_1"
}
```

Respuesta:

```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresAt": "ISO-8601",
  "user": {}
}
```

### Crear color

```http
POST /api/admin/colors
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "Negro",
  "desc": "Negro clásico",
  "code": "#000000"
}
```

### Crear producto

```http
POST /api/admin/products
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "targetGender": "UNISEX",
  "name": "Remera",
  "shortDesc": "Remera básica",
  "longDesc": "Remera de algodón"
}
```

El catálogo usa el enum real `TargetGender`; consulte el controlador para los valores disponibles del enum.

### Crear variante

```http
POST /api/variants
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "productId": "00000000-0000-0000-0000-000000000000",
  "colorId": 1,
  "size": "T_38",
  "desc": "Negro talle 38",
  "sku": "REM-T38-NEG",
  "price": 10000.00,
  "stock": 20
}
```

### Checkout invitado

```http
POST /api/sales/checkout
Content-Type: application/json

{
  "customerName": "Cliente Invitado",
  "customerEmail": "guest@example.com",
  "customerPhone": "1112345678",
  "shippingAddress": "Calle 1 123",
  "customerNotes": "",
  "isPickup": true,
  "shippingProvince": null,
  "shippingLocality": null,
  "shippingPostalCode": null,
  "paymentMethod": "EFECTIVO",
  "customerTaxIdType": null,
  "customerTaxIdNumber": null,
  "items": [
    {
      "variantId": 1,
      "quantity": 1
    }
  ]
}
```

## Manejo global de excepciones

Todas las excepciones controladas devuelven:

```json
{
  "timestamp": "ISO-8601 Instant",
  "status": 0,
  "error": "CODIGO",
  "detail": "mensaje",
  "path": "/api/ruta"
}
```

| Excepción / entrada | HTTP | `error` | Situación |
|---|---:|---|---|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_FAILED` | Validación `@Valid` |
| `MethodArgumentTypeMismatchException` | 400 | `INVALID_PARAMETER` | ID/UUID con formato inválido |
| `MissingServletRequestParameterException` | 400 | `INVALID_PARAMETER` | Falta un query parameter requerido |
| `InvalidProductCatalogSortException` | 400 | `INVALID_PARAMETER` | Orden de catálogo inválido |
| `InvalidStatusTransitionException` | 400 | `INVALID_STATUS_TRANSITION` | Cambio de estado directo inválido |
| `ReturnItemQuantityExceededException` | 400 | `RETURN_QUANTITY_EXCEEDED` | Devolución supera cantidad entregada |
| `MissingTaxIdentificationException` | 400 | `TAX_IDENTIFICATION_REQUIRED` | Faltan datos fiscales obligatorios |
| `InvalidCredentialsException` | 401 | `UNAUTHORIZED` | Login/sesión/refresh inválido o expirado |
| `InvalidWebhookSignatureException` | 401 | `INVALID_WEBHOOK_SIGNATURE` | Firma Mercado Pago inválida |
| `EmailNotVerifiedException` | 403 | `EMAIL_NOT_VERIFIED` | Cuenta pendiente de verificación |
| `ExpiredPasswordException` | 403 | `PASSWORD_EXPIRED` | Contraseña vencida |
| `AccessDeniedException` | 403 | `UNAUTHORIZED` | Permiso denegado |
| `ResourceNotFoundException` y subclasses | 404 | `RESOURCE_NOT_FOUND` | Entidad inexistente |
| `ResourceAlreadyExistsException` y subclasses | 409 | `RESOURCE_ALREADY_EXISTS` | Duplicado |
| `DataIntegrityViolationException` | 409 | `RESOURCE_ALREADY_EXISTS` | Integridad de datos |
| `InsufficientStockException` | 409 | `INSUFFICIENT_STOCK` | Stock insuficiente |
| `SaleNotEligibleForReturnException` | 409 | `SALE_NOT_ELIGIBLE_FOR_RETURN` | Venta no entregada/no elegible |
| `ActiveReturnAlreadyExistsException` | 409 | `ACTIVE_RETURN_ALREADY_EXISTS` | Reclamo activo duplicado |
| `ResourceNoLongerAvailableException` y subclasses | 410 | `RESOURCE_NO_LONGER_AVAILABLE` | Recurso eliminado/inactivo |
| `MinimumWholesalerQuantityException` | 422 | `MINIMUM_WHOLESALE_QUANTITY_NOT_MET` | Mayorista debajo de 10 unidades |
| `InvalidSaleStatusTransitionException` | 422 | `INVALID_STATUS_TRANSITION` | Transición de venta inválida |
| `EmptySaleItemsException` | 422 | `EMPTY_SALE_ITEMS` | Checkout sin ítems |
| `PaymentAmountMismatchException` | 422 | `PAYMENT_AMOUNT_MISMATCH` | Monto notificado no coincide |
| `ReturnWindowExpiredException` | 422 | `RETURN_WINDOW_EXPIRED` | Devolución fuera de 10 días |
| `InvalidFiscalOperationException` | 422 | `INVALID_FISCAL_OPERATION` | Precondición fiscal inválida |
| `InvalidUserDataException` | 422 | `INVALID_USER_DATA` | Datos de usuario inválidos |
| `ArcaBusinessRejectionException` | 422 | `ARCA_BUSINESS_REJECTION` | Rechazo de negocio WSFE |
| `InvalidSaleAmountException` | 422 | `INVALID_SALE_AMOUNT` | Total/subtotal inválido |
| `InvalidReturnRefundException` | 422 | `INVALID_RETURN_REFUND` | Reembolso inconsistente |
| `InvalidReturnInspectionException` | 422 | `INVALID_RETURN_INSPECTION` | Peritaje inconsistente |
| `InvalidReturnItemException` | 422 | `INVALID_RETURN_ITEM` | Ítem de devolución inválido |
| `InvalidShippingDestinationException` | 422 | `INVALID_SHIPPING_DESTINATION` | Provincia/localidad/CP inválidos |
| `InvalidShippingPaymentCombinationException` | 422 | `INVALID_SHIPPING_PAYMENT_COMBINATION` | Combinación envío/pago inválida |
| `AccountLockedException` | 423 | `ACCOUNT_LOCKED` | Cuenta bloqueada por fuerza bruta |
| `InvalidCatalogDataException` | 500 | `INVALID_CATALOG_DATA` | Datos inválidos al construir catálogo |
| Otra `Exception` | 500 | `INTERNAL_SERVER_ERROR` | Error no controlado |
| `StorageOperationException` | 502 | `STORAGE_SERVICE_ERROR` | Supabase Storage no disponible |
| `MercadoPagoIntegrationException` | 502 | `MERCADO_PAGO_INTEGRATION_ERROR` | Mercado Pago no disponible |
| `ArcaUnavailableException` | 502 | `ARCA_SERVICE_UNAVAILABLE` | WSAA/WSFE no disponible |

No existe manejador específico HTTP 503 ni código `503`.

Excepciones personalizadas por módulo:

- Common: `AccessDeniedException`, `InsufficientStockException`, `InvalidCatalogDataException`, `InvalidCredentialsException`, `InvalidStatusTransitionException`, `ResourceAlreadyExistsException`, `ResourceNoLongerAvailableException`, `ResourceNotFoundException`.
- Auth: `AccountLockedException`, `EmailNotVerifiedException`, `ExpiredPasswordException`.
- User: `InvalidUserDataException`, `UserAlreadyExistsException`, `UserNotFoundException`.
- Product: `InvalidProductCatalogSortException`, `ProductAlreadyExistsException`, `ProductNoLongerAvailableException`, `ProductNotFoundException`.
- Color: `ColorAlreadyExistsException`, `ColorNotFoundException`.
- Variant: `VariantAlreadyExistsException`, `VariantNoLongerAvailableException`, `VariantNotFoundException`.
- Image: `ImageCollectionNotAvailableException`, `ImageNotFoundException`, `StorageOperationException`.
- Cart: `CartItemNotFoundException`.
- Shipping: `InvalidShippingDestinationException`, `ShippingZoneNotFoundException`.
- Sale: `EmptySaleItemsException`, `InvalidSaleAmountException`, `InvalidSaleStatusTransitionException`, `InvalidShippingPaymentCombinationException`, `InvalidWebhookSignatureException`, `MercadoPagoIntegrationException`, `MinimumWholesalerQuantityException`, `MissingTaxIdentificationException`, `PaymentAmountMismatchException`, `PaymentNotFoundException`, `SaleNotFoundException`.
- Sale Return: `ActiveReturnAlreadyExistsException`, `InvalidReturnInspectionException`, `InvalidReturnItemException`, `InvalidReturnRefundException`, `InvalidReturnStatusTransitionException`, `ReturnItemQuantityExceededException`, `ReturnWindowExpiredException`, `SaleNotEligibleForReturnException`, `SaleReturnNotFoundException`.
- Fiscal: `ArcaBusinessRejectionException`, `ArcaUnavailableException`, `FiscalReceiptAlreadyExistsException`, `FiscalReceiptNotFoundException`, `InvalidFiscalOperationException`.
