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

Si se usa el token USER en `/api/test/admin`, Spring Security debe responder `403 Forbidden`. Si no se envía token a una ruta protegida, debe responder `401 Unauthorized`.

## 4. Estructura
- `entity`: entidades JPA User y Role.
- `repository`: consultas a la base de datos.
- `dto`: objetos de entrada y respuesta.
- `service`: carga de usuarios para Spring Security.
- `jwt`: generación, validación y filtro del token.
- `config`: configuración de seguridad y carga inicial de roles.
- `controller`: endpoints de autenticación y endpoints de prueba.

## Nota de seguridad
El laboratorio permite seleccionar roles desde el endpoint público de registro para reproducir sus ejemplos. En un sistema real no se debe permitir que cualquier persona se registre como ADMIN; ese rol debe asignarse únicamente mediante un proceso administrativo protegido. Cambia también la clave JWT y las credenciales de base de datos antes de desplegar.
