# Post-contenido – Unidad 9: Seguridad en Aplicaciones Web

## Descripción

Repositorio del laboratorio de la Unidad 9 de Programación Web. Contiene **un único proyecto Maven Spring Boot** (`seguridad-web/`) con **dos mecanismos de autenticación que conviven**:

- **Parte 1 – Sesión (Spring Security form-login)** para las vistas Thymeleaf: registro, login, BCrypt, roles `ADMIN`/`USER`, CSRF activo y verificación activa de XSS, SQL Injection y CSRF.
- **Parte 2 – JWT stateless** para la API REST bajo `/api/**`: `POST /api/auth/login` emite un token firmado y `JwtAuthFilter` lo valida en cada petición.

El dominio es deliberadamente pequeño: un `Usuario` (rol `ROLE_ADMIN` o `ROLE_USER`) y una `Tarea` que cada usuario crea y que un ADMIN puede administrar. Así la evaluación se centra en la capa de seguridad y no en el modelo de datos.

| Dato | Valor |
|---|---|
| Proyecto Maven | `seguridad-web` |
| Group / Artifact | `com.universidad` / `seguridad` |
| Paquete raíz | `com.universidad.seguridad` |

## Tecnologías

- Java 17
- Spring Boot 3.2.12: Web, Data JPA, Thymeleaf, Validation
- Spring Security 6.2, con `thymeleaf-extras-springsecurity6`
- `BCryptPasswordEncoder` con factor de trabajo 12
- jjwt 0.12.5 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- MySQL 8 (driver `mysql-connector-j`)
- Maven, incluido el wrapper `./mvnw`
- Pruebas: JUnit 5, Spring Security Test, MockMvc y H2 (solo para las pruebas automatizadas)

## Estructura del proyecto

```
naranjo-post1-u9/
├── README.md
├── .env.example                 # plantilla de variables de entorno (sin secretos)
├── docs/sql/admin.sql           # creación de la BD y del usuario ADMIN
├── capturas/                    # evidencias del laboratorio
└── seguridad-web/
    ├── pom.xml, mvnw, .mvn/
    └── src/main/java/com/universidad/seguridad/
        ├── config/      SecurityConfig (2 SecurityFilterChain), AdminInitializer
        ├── controller/  AuthController, TareaController (web)
        │                ApiAuthController, ApiTareaController, ApiAdminController (REST)
        ├── service/     UsuarioService, TareaService (@PreAuthorize)
        ├── repository/  UsuarioRepository, TareaRepository (@Query parametrizado)
        ├── model/       Usuario, Tarea
        ├── security/    UsuarioDetailsService, JwtUtil, JwtAuthFilter,
        │                TareaSeguridad, ApiErrorWriter
        ├── dto/         RegistroForm, TareaForm, TareaDTO, UsuarioDTO
        └── exception/   EmailDuplicadoException, RecursoNoEncontradoException,
                         ApiExceptionHandler
```

Todas las dependencias se inyectan por constructor.

## Requisitos previos

- JDK 17 o superior
- MySQL 8 en ejecución
- Git
- Maven no es necesario, porque se usa `./mvnw`
- `curl` para probar la API

## Configuración de MySQL

Como `root` en MySQL, cree la base de datos y el usuario de la aplicación con una contraseña propia:

```sql
CREATE DATABASE IF NOT EXISTS seguridad_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY '<CONTRASENA_MYSQL>';
GRANT ALL PRIVILEGES ON seguridad_db.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

Las tablas `usuarios` y `tareas` las crea Hibernate la primera vez que arranca la aplicación (`spring.jpa.hibernate.ddl-auto=update`).

### Variables de entorno (secretos fuera del repositorio)

`application.properties` **no contiene ninguna contraseña ni clave**: las lee de variables de entorno.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_PASSWORD` | Sí | Contraseña del usuario MySQL |
| `DB_USERNAME` | No | Usuario MySQL (por defecto `appuser`) |
| `DB_URL` | No | URL JDBC (por defecto `jdbc:mysql://localhost:3306/seguridad_db...`) |
| `JWT_SECRET` | Sí | Clave HMAC para firmar los JWT, **mínimo 32 caracteres** |
| `JWT_EXPIRACION_MS` | No | Expiración del token en ms (por defecto `3600000`, es decir 1 hora) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | No | Crean el ADMIN al arrancar (alternativa opcional) |

```bash
cp .env.example .env
```

Edite `.env` con sus valores. Para generar una clave JWT:

```bash
openssl rand -base64 48
```

Para cargar las variables en la terminal:

```bash
set -a; source .env; set +a
```

`.env` está en `.gitignore`. En Windows (PowerShell) se definen con `$env:DB_PASSWORD="..."`.

## Instalación y ejecución (cómo compilar y ejecutar)

```bash
git clone https://github.com/JimmyN44/naranjo-post1-u9.git
```

```bash
cd naranjo-post1-u9/seguridad-web
```

Con las variables de entorno cargadas:

```bash
./mvnw spring-boot:run
```

- Vistas web: http://localhost:8080/login
- API: `POST http://localhost:8080/api/auth/login`

Para ejecutar las pruebas automatizadas (usan H2, no necesitan MySQL):

```bash
./mvnw test
```

## Creación del usuario ADMIN

El registro público siempre asigna `ROLE_USER`. El ADMIN se crea de una de estas dos formas.

**A. Manual, como indica la guía (Parte 1, Paso 8).**

1. Arranque la aplicación una vez para que se creen las tablas.
2. Genere el hash BCrypt (factor 12). La contraseña se pasa por línea de comandos y no queda escrita en el código:
   ```bash
   ./mvnw test -Dtest=GenerarHashTest -Dpassword='<CONTRASENA_ADMIN>'
   ```
   La consola muestra `HASH BCRYPT: $2a$12$...`.
3. Inserte el registro (plantilla completa en `docs/sql/admin.sql`):
   ```sql
   INSERT INTO usuarios (nombre, email, contrasenia, rol, activo)
   VALUES ('Administrador', 'admin@universidad.edu', '<HASH_GENERADO>', 'ROLE_ADMIN', 1);
   ```

**B. Automática (opcional).** Defina `ADMIN_EMAIL` y `ADMIN_PASSWORD` antes de arrancar. `AdminInitializer` crea el ADMIN con la contraseña hasheada, solo si el correo no existe.

---

## Parte 1 – Login con sesión, BCrypt, roles y mitigación activa

`SecurityConfig` define `webFilterChain` con `@Order(2)` y `securityMatcher("/**")`:

| Ruta | Acceso |
|---|---|
| `/`, `/login`, `/registro`, `/css/**`, `/js/**`, `/error` | Público |
| `/admin/**` | Solo `ROLE_ADMIN` (403 para USER) |
| Resto (`/dashboard`, `/tareas/**`) | Autenticado |

- **Login por formulario:** `UsuarioDetailsService` busca el usuario por email en MySQL y `DaoAuthenticationProvider` compara la contraseña con `BCryptPasswordEncoder(12)`. Los hashes empiezan por `$2a$12$`. Un usuario con `activo = false` no puede entrar.
- **Logout:** solo por `POST /logout` con token CSRF. Invalida la sesión, borra `JSESSIONID` y redirige a `/login?logout`.
- **CSRF activo**, que es el valor por defecto. Thymeleaf agrega el campo `_csrf` en cada formulario con `th:action`.
- **Autorización en dos niveles:** reglas de URL en `HttpSecurity` y `@PreAuthorize` en los servicios, como defensa en profundidad:
  - `UsuarioService.listarTodos()` → `hasRole('ADMIN')`
  - `TareaService` → ADMIN gestiona todas las tareas; USER solo las suyas, mediante `@tareaSeguridad.esMismoUsuario(...)` y `@tareaSeguridad.esPropietarioDeTarea(...)`.
- **Vistas:**
  - `auth/login` y `auth/registro`.
  - `dashboard`, con el nombre del usuario mostrado mediante `th:text` y el enlace de administración visible solo con `sec:authorize="hasRole('ADMIN')"`.
  - `tareas/lista`: listar, crear, buscar, completar y eliminar.
  - `admin/panel`: usuarios y todas las tareas.
  - `error/403`.

## Parte 2 – API REST stateless protegida con JWT

`SecurityConfig` agrega `apiFilterChain` con `@Order(1)` y `securityMatcher("/api/**")`:

- `SessionCreationPolicy.STATELESS`: nunca se crea ni se lee una `HttpSession`.
- **CSRF desactivado solo en esta cadena.**
- `JwtAuthFilter` (`OncePerRequestFilter`) se registra antes de `UsernamePasswordAuthenticationFilter`. Lee `Authorization: Bearer <token>`, lo valida con `JwtUtil` y llena el `SecurityContext` sin consultar la base de datos. Si el token falta o no es válido, no autentica y no lanza excepción: la decisión se toma en `authorizeHttpRequests`.
- `JwtUtil` firma con HMAC-SHA usando la clave de `JWT_SECRET` e incluye `sub` (email), `rol`, `iat` y `exp` (1 hora por defecto). `esValido()` rechaza firmas inválidas, tokens expirados y tokens malformados.
- Respuestas de error en JSON: **401** sin token o con token inválido/expirado; **403** con token válido pero sin el rol requerido.

### Endpoints REST

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Recibe `{email, contrasenia}` y devuelve `{token, email, rol}`. Credenciales inválidas → 401 |
| GET | `/api/tareas` | Autenticado | Tareas del usuario del token |
| GET | `/api/tareas/buscar?texto=` | Autenticado | Búsqueda parametrizada en las tareas propias |
| POST | `/api/tareas` | Autenticado | Crea una tarea propia `{titulo, descripcion}` → 201 |
| PATCH | `/api/tareas/{id}/completar` | Propietario o ADMIN | Cambia el estado (403 si la tarea es ajena) |
| DELETE | `/api/tareas/{id}` | Propietario o ADMIN | Elimina → 204 (403 si la tarea es ajena) |
| GET | `/api/admin/usuarios` | ADMIN | Usuarios, **sin** el hash de la contraseña |
| GET | `/api/admin/tareas?texto=` | ADMIN | Todas las tareas, con búsqueda opcional |

Las respuestas usan DTO (`TareaDTO`, `UsuarioDTO`), así que el hash BCrypt nunca se serializa.

### Ejemplos de peticiones

**1. Login: obtener el token.**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@universidad.edu","contrasenia":"<CONTRASENA_ADMIN>"}'
```

Respuesta: `{"token":"eyJhbGciOi...","email":"admin@universidad.edu","rol":"ROLE_ADMIN"}`

```bash
TOKEN=<pegar_el_token>
```

**2. Petición autenticada.** Respuesta esperada: 200 con la lista de tareas.

```bash
curl -i http://localhost:8080/api/tareas -H "Authorization: Bearer $TOKEN"
```

**3. Sin token.** Respuesta esperada: 401.

```bash
curl -i http://localhost:8080/api/tareas
```

**4. Token alterado.** Respuesta esperada: 401.

```bash
curl -i http://localhost:8080/api/tareas -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.token.modificado"
```

**5. Crear una tarea sin token CSRF.** Respuesta esperada: 201.

```bash
curl -i -X POST http://localhost:8080/api/tareas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"titulo":"Tarea via API","descripcion":"JWT"}'
```

**6. Token expirado.** Arranque la aplicación con una expiración corta:

```bash
JWT_EXPIRACION_MS=5000 ./mvnw spring-boot:run
```

Obtenga un token, espere más de 5 segundos y repita la petición 2. Respuesta esperada: 401.

## Por qué dos mecanismos en el mismo proyecto

Las vistas Thymeleaf sirven a un **navegador que mantiene una cookie de sesión**. Ese es el mecanismo correcto para una aplicación web tradicional, y en ese escenario CSRF debe seguir activo, porque el navegador adjunta la cookie automáticamente incluso en peticiones provocadas por un sitio malicioso.

La API sirve a **clientes sin cookies**: una app móvil, una SPA en otro dominio o un servicio de terceros. Necesitan una credencial autocontenida en cada petición. El JWT viaja en el header `Authorization`, que un formulario cross-site no puede fijar. Por eso desactivar CSRF es seguro **solo** en `apiFilterChain`, que es stateless.

Ningún mecanismo reemplaza al otro: cada uno cubre un tipo de cliente distinto del mismo sistema. Las dos cadenas tienen `@Order` explícito:

1. `/api/**` (`@Order(1)`, la más específica) se evalúa primero.
2. Todo lo demás cae en la cadena de sesión (`@Order(2)`).

Así una petición a `/api/**` nunca termina en el formulario de login, sino en un 401 en JSON. Además, la cookie de sesión no autentica en la API y el JWT no autentica en las vistas web.

## Pruebas de seguridad

### Verificación manual (checkpoints de la guía)

| Checkpoint | Prueba | Resultado esperado |
|---|---|---|
| P1-CP1 | Ir a `/dashboard` sin sesión | Redirección a `/login` |
| P1-CP1 | Registrarse y revisar MySQL: `SELECT LEFT(contrasenia,7) FROM usuarios;` | `$2a$12$` |
| P1-CP2 | Entrar a `/admin` como USER | 403 |
| P1-CP2 | Entrar a `/admin` como `admin@universidad.edu` | Lista de usuarios |
| P1-CP3 | Ejecutar el comando CSRF de abajo | 403 Forbidden |
| P1-CP3 | Pulsar "Cerrar Sesión" en el dashboard | Redirección a `/login?logout` |
| P2-CP1 | `POST /api/auth/login` con credenciales válidas | JSON con token. Con contraseña incorrecta: 401 sin token |
| P2-CP2 | `GET /api/tareas` | 200 con token; 401 sin token; 401 con token alterado o expirado |
| P2-CP3 | Usar `/dashboard` en el navegador mientras se ejecuta curl contra `/api/**` | Ambas superficies responden a la vez |

Comando de la prueba CSRF (P1-CP3). Se espera 403 Forbidden:

```bash
curl -i -X POST http://localhost:8080/logout
```

### XSS: `th:text` frente a `th:utext`

1. Registrar un usuario cuyo nombre sea `<script>alert("XSS")</script>`.
2. Con la versión del repositorio (`th:text`), el dashboard muestra el texto literal escapado (`&lt;script&gt;...`) y **no** se ejecuta nada.
3. **Demostración temporal:** en `seguridad-web/src/main/resources/templates/dashboard.html`, justo antes de `<p id="bienvenida">`, agregar:
   ```html
   <p th:utext="${usuario}"></p>
   ```
   Reiniciar, recargar `/dashboard` y comprobar que **sí** aparece la alerta.
4. **Revertir el cambio de inmediato.** La versión final del repositorio no contiene `th:utext`. La prueba `ningunaPlantillaUsaThUtext` falla si alguna plantilla lo incluye, lo que evita confirmar la línea temporal por error.

Las notas para el desarrollador en las plantillas son comentarios de Thymeleaf (`<!--/* ... */-->`). A diferencia de los comentarios HTML, no se envían al navegador.

En todas las plantillas, los campos escritos por usuarios (nombre, email, título y descripción de tareas) se muestran con `th:text`.

### SQL Injection: `@Query` parametrizado

En el buscador de `/tareas`, buscar `' OR '1'='1`. Resultado: **0 tareas**, porque el valor viaja como parámetro (`PreparedStatement`) y se compara como texto literal.

Con la versión concatenada, que aparece **solo en un comentario** de `TareaRepository`, el SQL resultante habría sido:

```sql
SELECT * FROM tareas WHERE titulo LIKE '%' OR '1'='1%'
```

La condición siempre verdadera habría devuelto **todas las tareas de todos los usuarios**, sin filtrar por título. Además, la búsqueda de un USER está restringida a sus propias tareas (`buscarPorTituloYPropietario`).

### Pruebas automatizadas (`./mvnw test`)

| Clase | Qué verifica |
|---|---|
| `JwtUtilTest` | Generación de tokens con email y rol; rechazo de firma alterada, payload falsificado, otra clave, token expirado y token malformado; clave mínima de 32 bytes |
| `SesionSeguridadTest` | Registro con BCrypt `$2a$12$` y rol USER; bloqueo de *mass assignment*; email duplicado; login correcto e incorrecto; usuario inactivo; 403 en `/admin` para USER; aislamiento de tareas entre usuarios; ADMIN gestiona todas las tareas; XSS escapado (ninguna etiqueta `<script>` llega al HTML, ni en el dashboard ni en el panel admin) y ninguna plantilla con `th:utext`; SQLi en repositorio y en buscador; CSRF en logout y formularios |
| `ApiJwtSeguridadTest` | Login JWT (200 / 401); token válido, ausente, alterado, malformado, expirado y de otra clave; API sin sesión; roles en `/api/admin/**`; DTO sin hashes; propiedad de tareas; coexistencia sesión + JWT; CSRF activo en la web y desactivado en la API |
| `GenerarHashTest` | Utilidad para el hash del ADMIN; solo se ejecuta con `-Dpassword` |

## Decisiones de diseño y correcciones a la guía

| Fragmento de la guía | Problema | Solución aplicada |
|---|---|---|
| `ApiAuthController` inyecta `AuthenticationManager` | No había bean, y la aplicación no arrancaba | `@Bean AuthenticationManager` sobre el mismo `DaoAuthenticationProvider` |
| `apiFilterChain` sin punto de entrada | Sin token respondía **403**, no 401 | `exceptionHandling` con 401 y 403 en JSON (`ApiErrorWriter`) |
| `#usuarioId == authentication.principal.id` | El principal (`User` o `String`) no tiene `id`, y la evaluación fallaba | Bean `tareaSeguridad` que compara por `authentication.name` y sirve para sesión y JWT |
| Dashboard con `auth.getName()` | Es el email (validado con `@Email`), así que la prueba XSS no se podía reproducir | Se muestra el `nombre` del usuario |
| `ApiTareaController` con `usuario = null` y entidad expuesta | `NullPointerException`; se serializaba el hash del propietario | Búsqueda por email y `TareaDTO` |
| `@ModelAttribute Usuario` en el registro | *Mass assignment*: `id`, `rol` y `activo` se podían enviar desde el formulario | DTO `RegistroForm` con solo nombre, email y contraseña |
| `JwtAuthFilter` con `@Component` | Spring Boot lo registraba también como filtro global | `FilterRegistrationBean.setEnabled(false)` |
| `buscar()` sin filtro de propietario | Un USER veía tareas ajenas | `buscarPorTituloYPropietario` para USER; búsqueda global solo para ADMIN |
| `/` y `/error` | `/` sin controlador; `/error` protegido | Redirección `/` → `/dashboard`; `/error` público; página 403 propia |

### Nota de seguridad para producción

La guía escribe la clave HMAC directamente en `JwtUtil` por simplicidad. **Este proyecto no lo hace**: la clave se lee de la variable de entorno `JWT_SECRET`, y las credenciales de MySQL y del ADMIN también vienen del entorno.

Si la clave quedara expuesta en el repositorio, cualquiera con acceso al código podría firmar tokens válidos, por ejemplo con `rol=ROLE_ADMIN`. En producción conviene además:

- usar un gestor de secretos;
- rotar la clave;
- servir todo por HTTPS.

Hay una limitación conocida del enfoque stateless: un token emitido sigue siendo válido hasta que expira, aunque el usuario sea desactivado después.

## Capturas de pantalla

> Las capturas deben tomarse sobre la aplicación en ejecución y guardarse en `capturas/` con estos nombres. 

![Formulario de login](capturas/login.png)
![Dashboard con th:text seguro](capturas/dashboard-seguro.png)
![XSS ejecutado con th:utext (prueba temporal)](capturas/xss-utext.png)
![Búsqueda de tareas con @Query parametrizado](capturas/sql-injection-mitigado.png)
![CSRF rechazado (403)](capturas/csrf-403.png)
![POST /api/auth/login con token recibido](capturas/jwt-login.png)
![GET /api/tareas autenticado con Bearer token](capturas/jwt-aceptado.png)
![GET /api/tareas sin token (401)](capturas/jwt-rechazado.png)
