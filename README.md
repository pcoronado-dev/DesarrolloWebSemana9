# Laboratorio S09 - Autenticacion JWT con Spring Boot

## Requisitos
- Java 17 o superior
- Maven 3.8+
- MySQL 8+
- Postman

## 1. Crear la base de datos
Ejecutar en MySQL Workbench o phpMyAdmin:

```sql
CREATE DATABASE security_db;
```

Configurar usuario y contraseña de MySQL en `src/main/resources/application.properties`.

## 2. Ejecutar
Desde la carpeta del proyecto:

```bash
mvn spring-boot:run
```

Hibernate crea las tablas `users`, `roles` y `user_roles`. Al iniciar, `DataInitializer` registra los roles USER, ADMIN y MODERATOR si todavía no existen.

## 3. Probar en Postman

También puedes importar la colección `postman/LaboratorioS09-JWT.postman_collection.json` (Postman -> Import -> File). Ejecuta las peticiones en orden: los `signin` guardan automáticamente los tokens en las variables `tokenAdmin` y `tokenUser`.

### Registrar administrador
POST `http://localhost:8080/api/auth/signup`
Body -> raw -> JSON:

```json
{
  "username": "admin",
  "email": "admin@test.com",
  "password": "123456",
  "roles": ["ADMIN"]
}
```

### Registrar usuario
POST `http://localhost:8080/api/auth/signup`

```json
{
  "username": "user1",
  "email": "user@test.com",
  "password": "123456",
  "roles": ["USER"]
}
```

### Iniciar sesión
POST `http://localhost:8080/api/auth/signin`

```json
{
  "email": "admin@test.com",
  "password": "123456"
}
```

Copiar el valor de `accessToken`.

### Probar ruta pública
GET `http://localhost:8080/api/test/all`
No necesita token. Respuesta: `Public Content`.

### Probar ruta USER
GET `http://localhost:8080/api/test/user`
Authorization -> Bearer Token -> pegar el token del usuario `user@test.com`.
Respuesta: `User Content`.

### Probar ruta ADMIN
GET `http://localhost:8080/api/test/admin`
Authorization -> Bearer Token -> pegar el token de `admin@test.com`.
Respuesta: `Admin Content`.

Si se usa el token USER en `/api/test/admin`, Spring Security responde `403 Forbidden` con un JSON que indica que no hay permisos. Si no se envía token (o el token es inválido o expiró) a una ruta protegida, responde `401 Unauthorized`. Un login con email o password incorrecto también responde `401 Unauthorized`, y un body inválido responde `400 Bad Request`.

## 4. Estructura
- `entity`: entidades JPA User y Role.
- `repository`: consultas a la base de datos.
- `dto`: objetos de entrada y respuesta, con validación (`@NotBlank`, `@Email`, `@Size`).
- `service`: carga de usuarios para Spring Security.
- `jwt`: generación, validación y filtro del token.
- `config`: configuración de seguridad, respuestas JSON para 401/403 y carga inicial de roles.
- `exception`: manejo global de errores (`GlobalExceptionHandler`).
- `controller`: endpoints de autenticación y endpoints de prueba.
- `src/test`: pruebas automatizadas del token, del filtro, del login y de la protección de endpoints.
- `postman`: colección con las 11 peticiones del laboratorio.

## 5. Pruebas automatizadas
El proyecto incluye 22 pruebas con JUnit 5 que no necesitan MySQL:

```bash
mvn test
```

Cubren:
- Generación y validación del JWT (token válido, firma alterada, otra clave, token expirado, texto inválido).
- El filtro JWT (autentica con `Bearer`, ignora peticiones sin token o con token inválido).
- El login (`/api/auth/signin` devuelve token y roles; credenciales incorrectas lanzan `BadCredentialsException`).
- La protección de endpoints: público `200`, sin token `401`, rol insuficiente `403`, rol correcto `200`.
- La validación de los DTO de entrada.

## 6. Actividad de análisis - Seguridad JWT

### Caso 1: Ruta pública - `GET /api/test/all`
1. Devuelve `200 OK` con el texto `Public Content`.
2. En `src/main/java/com/tecsup/controller/TestController.java`, método `allAccess` (líneas 10-13).
3. El método `allAccess()`.
4. Porque la regla de seguridad la declara pública: no exige token ni usuario autenticado.
5. En `SecurityConfig.java:50`: `.requestMatchers("/api/test/all").permitAll()`.
6. Caería en `.anyRequest().authenticated()`: sin token respondería `401`; con un token válido seguiría respondiendo `200`.
7. `SecurityFilterChain` define el orden de los filtros y las reglas de autorización; el `FilterChainProxy` aplica esa cadena en cada petición (JWT → autenticación → autorización → controller).

### Caso 2: Acceso sin token - `GET /api/test/user`
1. Devuelve el JSON `{"status":401,"error":"Unauthorized","message":"Token ausente, invalido o expirado","path":"/api/test/user"}`.
2. `401 Unauthorized`.
3. La cadena `FilterChainProxy`/`SecurityFilterChain`; concretamente `AuthorizationFilter` detecta que falta autorización y `RestAuthenticationEntryPoint` escribe la respuesta.
4. Porque la petición queda como anónima: `JwtAuthFilter` no encontró token y el `SecurityContext` sigue vacío, así que no cumple la regla que exige rol USER o ADMIN.
5. No. `AuthorizationFilter` lanza `AccessDeniedException` antes del `DispatcherServlet`, por lo que el método del controller nunca se ejecuta.
6. `JwtAuthFilter` (no autentica), `AuthorizationFilter` (deniega) y `ExceptionTranslationFilter` (delega en el entry point).
7. La ausencia del header `Authorization: Bearer <token>`; sin autenticación, `.requestMatchers("/api/test/user").hasAnyRole("USER", "ADMIN")` no se satisface.

### Caso 3: Token inválido - `GET /api/test/user`
1. Devuelve `401 Unauthorized` con el mismo JSON del caso 2: "Token ausente, invalido o expirado".
2. `JwtUtils.validateJwtToken(String authToken)` (`JwtUtils.java:49`).
3. En `com.tecsup.jwt.JwtUtils`, invocado desde `JwtAuthFilter.java:37`.
4. JJWT (`io.jsonwebtoken:jjwt-api`, `jjwt-impl` y `jjwt-jackson` 0.11.5), declarada en `pom.xml`. Spring Boot/Spring Security no generan el JWT por sí solos.
5. `MalformedJwtException` si el formato es incorrecto, `SignatureException` si la firma no coincide, `ExpiredJwtException` si expiró, `UnsupportedJwtException` o `IllegalArgumentException` en otros casos. `validateJwtToken` las captura como `JwtException | IllegalArgumentException`.
6. Token inexistente: no hay header, `parseJwt` devuelve `null` y no se valida nada. Token inválido: el header sí existe, `validateJwtToken` intenta parsearlo y falla por formato, firma o expiración. Ambos casos terminan en `401`, pero por caminos distintos.
7. `parseJwt` extrae el token, `validateJwtToken` devuelve `false`, no se carga el usuario ni se setea la autenticación, se registra un `warn` y la cadena continúa con el contexto anónimo. Si se lanza una excepción no controlada, el `catch` limpia el `SecurityContextHolder`.

### Caso 4: Acceso con token válido - `GET /api/test/user`
1. Devuelve `200 OK` con `User Content`.
2. El método `userAccess()` (`TestController.java:15-18`).
3. Extrae el token del header, valida firma y expiración, carga el usuario desde la BD y registra su autenticación en el contexto de seguridad.
4. `UserDetailsServiceImpl.loadUserByUsername(email)` busca al usuario por email en `UserRepository` y construye un `UserDetailsImpl` con sus authorities (`ROLE_...`).
5. `UsernamePasswordAuthenticationToken`, guardado dentro del `SecurityContext` de `SecurityContextHolder`.
6. Flujo: `Postman` → header `Authorization: Bearer <token>` → `JwtAuthFilter.parseJwt` → `JwtUtils.validateJwtToken` y `getUserNameFromJwtToken` (subject = email) → `UserDetailsServiceImpl` carga al usuario → `UsernamePasswordAuthenticationToken` en `SecurityContextHolder` → `AuthorizationFilter` valida `hasAnyRole("USER","ADMIN")` → `TestController.userAccess()` → `User Content`.
7. Si el token expiró, `validateJwtToken` recibe `ExpiredJwtException`, devuelve `false`, no se autentica y la petición termina en `401`.

### Caso 5: Ruta ADMIN sin permisos - `GET /api/test/admin`
1. Devuelve `403 Forbidden` con el JSON `{"status":403,"error":"Forbidden","message":"No tiene permisos para acceder a este recurso","path":"/api/test/admin"}`.
2. `403 Forbidden`.
3. En `SecurityConfig.java:51`: `.requestMatchers("/api/test/admin").hasRole("ADMIN")`.
4. El método `hasRole("ADMIN")` dentro de `authorizeHttpRequests` (Spring Security exige la authority `ROLE_ADMIN`).
5. En `UserDetailsImpl.build` (`UserDetailsImpl.java:31-38`), que mapea cada rol de la BD a `new SimpleGrantedAuthority("ROLE_" + nombre)`.
6. Una `Collection<? extends GrantedAuthority>`; en la práctica, un `List<SimpleGrantedAuthority>`. El usuario USER tiene `ROLE_USER`.
7. `Authentication` responde quién es el usuario (identidad demostrada con el token); `Authorization` decide qué puede hacer (roles/permisos). Aquí la autenticación es correcta pero la autorización falla.
8. Porque `ROLE_USER` no satisface `hasRole("ADMIN")`: se lanza `AccessDeniedException`, `ExceptionTranslationFilter` detecta que el usuario sí está autenticado y llama a `RestAccessDeniedHandler`, que responde `403`.

### Caso 6: Ruta ADMIN con permisos - `GET /api/test/admin`
1. Devuelve `200 OK` con `Admin Content`.
2. El método `adminAccess()` (`TestController.java:20-23`).
3. Firma válida, token no expirado, usuario existente en la BD, autenticación establecida y authority `ROLE_ADMIN` presente.
4. Rol `ADMIN` (`ROLE_ADMIN`), asignado en el signup al usuario `admin@test.com`.
5. `SecurityContextHolder` contiene un `SecurityContext` con un `UsernamePasswordAuthenticationToken` autenticado, cuyo principal es el `UserDetailsImpl` del admin y cuyas authorities son `[ROLE_ADMIN]`. Ese contexto se limpia al terminar la petición.
6. Flujo completo:

```text
Postman
  └─ POST /api/auth/signin (email + password)
       └─ AuthController.authenticateUser
            ├─ AuthenticationManager + BCrypt (autentica)
            ├─ JwtUtils.generateJwtToken (firma HS256, exp = 1 hora)
            └─ 200 { accessToken }

Postman
  └─ GET /api/test/admin  +  Authorization: Bearer <token>
       └─ JwtAuthFilter
            ├─ parseJwt
            ├─ JwtUtils.validateJwtToken (firma + expiración)
            ├─ UserDetailsServiceImpl.loadUserByUsername(email)
            └─ SecurityContextHolder.setAuthentication(ROLE_ADMIN)
       └─ AuthorizationFilter: /api/test/admin -> hasRole("ADMIN") OK
       └─ TestController.adminAccess() -> "Admin Content" (200)
```

7. `JwtUtils.generateJwtToken(Authentication)`, invocado desde `AuthController.authenticateUser` (`AuthController.java:90`).

## Nota de seguridad
El laboratorio permite seleccionar roles desde el endpoint público de registro para reproducir sus ejemplos. En un sistema real no se debe permitir que cualquier persona se registre como ADMIN; ese rol debe asignarse únicamente mediante un proceso administrativo protegido. Cambia también la clave JWT y las credenciales de base de datos antes de desplegar.
