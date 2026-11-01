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

## Nota de seguridad
El laboratorio permite seleccionar roles desde el endpoint público de registro para reproducir sus ejemplos. En un sistema real no se debe permitir que cualquier persona se registre como ADMIN; ese rol debe asignarse únicamente mediante un proceso administrativo protegido. Cambia también la clave JWT y las credenciales de base de datos antes de desplegar.
