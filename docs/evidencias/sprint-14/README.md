# Evidencias del sprint 14

Verificación realizada el 3 de noviembre de 2026 (Argentina). Compilación con Java 21/Maven 3.9.16; ejecución en Temurin 25 sobre Linux, Spring Boot 4.1.0, Spring Cloud 2025.1.4 y JJWT 0.12.6. El entorno contiene cinco bases MySQL, Redis, cinco servicios de aplicación, Gateway, Eureka y Config Server. Los JSON usan fechas UTC.

| Verificación | Resultado | Evidencia |
| --- | --- | --- |
| Reactor Maven | Padre y ocho módulos; 103 pruebas aprobadas, sin fallos, errores ni omisiones | [Verificaciones del proyecto](sprint-14-verificaciones.json) |
| Colección completa | 119 solicitudes y 208 verificaciones aprobadas; cinco escenarios JWT y regresión funcional | [Colección](sprint-14-jwt.postman_collection.json) y [resultados Newman](sprint-14-jwt.newman-results.json) |
| Registro y login | HTTP 200, respuesta con token únicamente, HS256, sub/rol/iat/exp y duración de 24 horas | [Resultados y claims comprobados](sprint-14-jwt.newman-results.json) |
| Petición autenticada | Mascotas devuelve 200 con Bearer; el flujo de las cuatro APIs conserva sus respuestas | [Resultados Newman](sprint-14-jwt.newman-results.json) |
| Rechazos de JWT | 401 ante ausencia, expiración, clave incorrecta, firma ausente, payload alterado, nbf futuro, tipo de rol incorrecto o algoritmo diferente | [Casos negativos](sprint-14-jwt.newman-results.json) |
| Todas las entradas | APIs, aliases, versión, Actuator y ruta desconocida protegidos; /auth público y CORS preflight operativo | [Resultados Newman](sprint-14-jwt.newman-results.json) |
| Rechazo anterior al backend | 40 peticiones sin credenciales válidas devuelven 401; el contador de GET de Mascotas se mantiene en 14 | [Contadores medidos](sprint-14-seguridad.json) |
| BCrypt persistido | Cuatro usuarios de prueba con hashes de 60 caracteres, prefijo BCrypt, sin texto plano y con salts distintos | [Inspección de patitas_auth](sprint-14-seguridad.json) |
| Username único y concurrencia | Dos registros simultáneos producen 200/409 y una sola fila; índice único en MySQL | [Prueba de concurrencia](sprint-14-seguridad.json) |
| Rate Limiter autenticado | Ráfaga de 60 GET con Bearer: 20 aceptados, 40 HTTP 429 y recuperación tras regenerar tokens | [Ráfaga y resultados](sprint-14-seguridad.json) |
| Limpieza y operación | Cuatro usuarios propios eliminados, datos de negocio vacíos, seis aplicaciones UP y catorce contenedores saludables | [Verificación final](sprint-14-verificaciones.json) |

Las pruebas Maven incluyen las 55 anteriores y 48 nuevas. Comprueban BCrypt real con AuthenticationManager, errores de credenciales genéricos, conflictos con causa conservada, claves débiles, JWT expirados o malformados, rechazo de otro algoritmo, datos de identidad inválidos y el filtro reactivo. La cadena capturada en las pruebas demuestra que X-Auth-User reemplaza cabeceras falsas y que Authorization/X-Auth-Rol no se reenvían; las cadenas WebFilter y GlobalFilter validan una sola vez.

Para repetir la verificación, iniciar el ecosistema de Compose siguiendo el [README](../../../README.md), con las cuatro bases de negocio vacías:

```powershell
python docs/evidencias/sprint-14/verificar-seguridad.py
```

El [verificador](verificar-seguridad.py) espera que el login alcance ms-auth, genera un entorno Postman privado y ejecuta Newman. Usa nombres únicos; el valor predeterminado de la colección manual es jorge, como en la consigna. Las contraseñas de prueba usan el ejemplo 1234. Los casos de expiración y firma se construyen localmente con la clave del entorno; un control HS256 correcto responde 200 para comprobar que los casos negativos usan la misma configuración de firma.

Después ejecuta el registro concurrente y consulta MySQL para comprobar BCrypt e índice único. Mide el contador de Mascotas antes y después de 40 peticiones 401. Por último ejecuta una ráfaga autenticada mezclando la ruta canónica de Turnos y su alias. El bloque de limpieza elimina solamente los cuatro usernames propios y comprueba que la cantidad total de usuarios vuelve a su valor inicial.

Los tokens utilizables y la clave no se incluyen en los artefactos. Las respuestas guardan `[REDACTED]`, claims comprobados y fingerprints SHA-256; también se oculta el token de la prueba en query. El entorno privado y el reporte completo temporal se eliminan al terminar correctamente. Los resultados de esta ejecución restauran la base de usuarios a cero registros.

Los servicios de negocio permanecen sin validación JWT y sus puertos 8081–8084 no se publican. ms-auth también permanece interno en 8085. Gateway cubre Actuator con la misma autenticación; su healthcheck de Compose es TCP. El health HTTP autorizado, Redis y los seis registros Eureka se comprueban al finalizar.

La configuración está publicada en [ms-auth.yml](https://github.com/ggomarighetti/patitas-config/blob/6209ff5268f5282daef5de110f9ed9d2160b03f9/ms-auth.yml) y [api-gateway.yml](https://github.com/ggomarighetti/patitas-config/blob/6209ff5268f5282daef5de110f9ed9d2160b03f9/api-gateway.yml). El repositorio conserva únicamente `${JWT_SECRET}`; `.env` queda fuera de Git. Los bootstraps locales contienen nombre e import obligatorio de Config Server.

Se conserva sin modificar la [consigna original](../../sprints/Sprint_14_JWT_Seguridad.pdf). Las decisiones y referencias oficiales están en el [README](../../../README.md#autenticación-jwt--sprint-14). La entrega implementa autenticación y propagación de identidad; autorización por rol y refresh token son extensiones opcionales.
