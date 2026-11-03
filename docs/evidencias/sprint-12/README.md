# Evidencias del sprint 12

Verificación realizada el 2 de noviembre de 2026 (Argentina). Compilación con Java 21 y Maven 3.9.16; ejecución en Temurin 25 sobre Linux, Spring Boot 4.1.0, Spring Cloud 2025.1.4, Gateway 5.0.4 y cuatro bases MySQL 8.0.40 mediante Docker Compose. Los JSON usan fechas UTC.

| Verificación | Resultado | Evidencia |
| --- | --- | --- |
| Reactor Maven | Padre y siete módulos compilados con `mvnw.cmd -B clean verify`; 47 pruebas aprobadas, sin fallos, errores ni omisiones | [Verificaciones del proyecto](sprint-12-verificaciones.json) |
| Flujo público completo | 66 solicitudes y 131 verificaciones aprobadas, sin fallos; todas las llamadas de negocio pasan por 8080 | [Colección](sprint-12-gateway.postman_collection.json) y [resultados Newman](sprint-12-gateway.newman-results.json) |
| Registro | Gateway y los cuatro microservicios registrados como UP | [Dashboard Eureka](sprint-12-eureka-gateway.png) |
| Rutas | Cuatro rutas canónicas `lb://`, cuatro de discovery y una de versionado; nueve en total | [Resultados Newman](sprint-12-gateway.newman-results.json) |
| Balanceo real | Dos instancias de Dueños en Eureka reciben solicitudes mediante la misma ruta; después se restaura una instancia | [Destinos observados y recuperación](sprint-12-balanceo.json) |
| CORS | Preflight en las cuatro rutas y en una ruta inexistente; GET con origen de navegador; PATCH permitido y sin credenciales | [Resultados Newman](sprint-12-gateway.newman-results.json) |
| Alta de recursos | Los cuatro POST responden 201 y conservan `localhost:8080` en Location | [Cabeceras de respuesta](sprint-12-gateway.newman-results.json) |
| Logging | Método, ruta, estado efectivo, milisegundos, ruta elegida y destino; orden -1 y sin duplicados | [Muestra de logs](sprint-12-balanceo.json) y [pruebas Maven](sprint-12-verificaciones.json) |
| Configuración | Bootstrap local mínimo; puerto, Eureka, rutas, CORS y Actuator desde Git | [Configuración publicada](https://github.com/ggomarighetti/patitas-config/blob/883fa80bb6587636669f2f6c337c8293dbea6da0/api-gateway.yml) |
| Actuator | Health e info disponibles; inspección de rutas de solo lectura; refresh y env no expuestos (404) | [Resultados Newman](sprint-12-gateway.newman-results.json) |
| Aislamiento y servidor | 8081–8084 internos en Compose; Netty/WebFlux, sin MVC, servidor Tomcat, JPA ni driver MySQL en el gateway | [Verificaciones del proyecto](sprint-12-verificaciones.json) |
| Limpieza | Listas de las cuatro APIs vacías y una instancia por aplicación al finalizar | [Verificaciones del proyecto](sprint-12-verificaciones.json) |

Las 47 pruebas incluyen las 43 existentes y cuatro de LoggingFilter. Se verifican el estado 200 implícito, una sola pareja de logs al participar en WebFilter y GlobalFilter, la conservación del error original con el estado final 503 y la exclusión de cabeceras y valores de consulta. El hook anterior a la confirmación de la respuesta registra también los errores resueltos por el manejador externo.

Para repetir la colección, iniciar el ecosistema siguiendo el [README](../../../README.md) y usar las cuatro bases sin registros. La colección genera sus propios identificadores, valida CRUD, conflictos, errores de referencias y cancelación de turnos, y elimina los datos al terminar:

```powershell
npx newman run docs/evidencias/sprint-12/sprint-12-gateway.postman_collection.json
```

Las variables de negocio apuntan al gateway en 8080. Las consultas a 8761 y 8888 son controles administrativos de Eureka y Config Server. Los resultados archivados resumen la exportación JSON de Newman e incluyen estado, cuerpo, cabeceras relevantes y cada verificación.

La prueba de balanceo aumenta temporalmente `ms-dueno` a dos réplicas con Compose. Se espera su registro UP, se consulta repetidamente `/api/duenos` por 8080 y se comparan los destinos del log del gateway con las dos direcciones publicadas por Eureka. No se agregan rutas ni demoras artificiales a las aplicaciones. Tras reducir a una réplica se espera la actualización del registro y de la caché de discovery, y se requieren cuatro respuestas 200 consecutivas.

Al retirar una réplica pueden quedar destinos en caché durante la propagación de discovery. Los errores transitorios y tiempos de espera observados en esa recuperación se conservan en el JSON de balanceo. Este sprint no promete continuidad sin errores durante una caída ni incorpora los mecanismos de resiliencia del sprint 13.

La configuración usa el prefijo vigente `spring.cloud.gateway.server.webflux`, el starter reactivo actual y `management.endpoint.gateway.access: read-only`. Discovery Locator excluye al propio gateway; sus prefijos públicos son minúsculos aunque los IDs automáticos puedan contener el nombre del servicio en mayúsculas. El alias opcional `/v1/duenos` reescribe tanto la ruta base como los identificadores. [Manual oficial de Gateway](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux.html).

Se conserva el PDF de la [consigna original](../../sprints/Sprint_12_APIGateway.pdf). Se mantienen las bases MySQL existentes; Circuit Breaker, JWT y rate limiting quedan para sus sprints correspondientes.
