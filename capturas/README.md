# Capturas del laboratorio

Coloque aquí las capturas tomadas sobre la aplicación en ejecución, con estos nombres exactos (los referencia el README principal):

| Archivo | Qué debe mostrar |
|---|---|
| `login.png` | Formulario de login (`/login`) |
| `dashboard-seguro.png` | Dashboard con el nombre `<script>alert("XSS")</script>` mostrado como texto (`th:text`) |
| `xss-utext.png` | Alerta ejecutada con la línea `th:utext` agregada temporalmente |
| `sql-injection-mitigado.png` | Buscador de `/tareas` con `' OR '1'='1` → 0 resultados |
| `csrf-403.png` | `curl -i -X POST http://localhost:8080/logout` → 403 Forbidden |
| `jwt-login.png` | `POST /api/auth/login` con el token recibido |
| `jwt-aceptado.png` | `GET /api/tareas` con `Authorization: Bearer` → 200 |
| `jwt-rechazado.png` | `GET /api/tareas` sin token → 401 |
