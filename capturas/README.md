# Capturas del laboratorio

Evidencias de la aplicación en ejecución contra MySQL 8, referenciadas desde el README principal.

| Archivo | Qué muestra |
|---|---|
| `login.png` | Formulario de login (`/login`) |
| `dashboard-seguro.png` | Dashboard de un usuario cuyo nombre es `<script>alert("XSS")</script>`: con `th:text` se muestra como texto y no se ejecuta |
| `xss-utext.png` | El mismo usuario con la línea `th:utext` agregada temporalmente: el navegador **ejecuta** `alert("XSS")` |
| `sql-injection-mitigado.png` | Buscador de `/tareas` (como ADMIN) con `' OR '1'='1` → 0 resultados, aunque existen 3 tareas en la base de datos |
| `csrf-403.png` | `curl -i -X POST http://localhost:8080/logout` sin token CSRF → `403` |
| `jwt-login.png` | `POST /api/auth/login` → JSON con `token`, `email` y `rol` |
| `jwt-aceptado.png` | `GET /api/tareas` con `Authorization: Bearer $TOKEN` → `200` |
| `jwt-rechazado.png` | `GET /api/tareas` sin token → `401` |

## Cómo se obtuvieron

Las capturas se generaron automáticamente, con ayuda de Claude Code, sobre la aplicación real (código del commit `84bb67e`) ejecutándose contra MySQL 8.0:

- **Navegador:** Chromium controlado con Playwright en una pantalla virtual (Xvfb); se fotografió la pantalla completa, incluida la barra de direcciones y la alerta nativa del navegador.
- **Terminal:** cada comando `curl` se ejecutó en una terminal xterm real y se fotografió su ventana. En `jwt-login.png` las credenciales se envían desde un archivo (`-d @login.json`) para no mostrar la contraseña del ADMIN.
- **XSS con `th:utext`:** se usó una **copia temporal** del proyecto con la línea insegura, ejecutada en el puerto `8082` (por eso esa URL en la captura). El código del repositorio no contiene `th:utext`.

Para repetirlas manualmente, siga la sección "Pruebas de seguridad" del README principal.
