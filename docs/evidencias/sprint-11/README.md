# Evidencias del sprint 11

Verificación realizada el 2 de noviembre de 2026. Compilación con Java 21 y Maven 3.9.16; ejecución en Temurin 25 sobre Linux, Spring Boot 4.1.0, Spring Cloud 2025.1.4 y cuatro bases MySQL 8.0.40 mediante Docker Compose.

| Verificación | Resultado | Evidencia |
| --- | --- | --- |
| Reactor Maven | Padre y seis módulos compilados; 43 pruebas aprobadas, sin fallos, errores ni omisiones | [Resumen del proyecto](sprint-11-verificaciones.json) |
| Flujo completo | 59 solicitudes y 121 verificaciones aprobadas, sin fallos | [Colección](sprint-11-openfeign.postman_collection.json) y [resultados Newman](sprint-11-openfeign.newman-results.json) |
| Registro de servicios | Dueños, Mascotas, Veterinarios y Turnos registrados como UP | [Dashboard Eureka](sprint-11-eureka-servicios.png) |
| Referencias existentes | POST de Turno devuelve 201 y estado PENDIENTE después de consultar ambos servicios por Feign | [Resultados Newman](sprint-11-openfeign.newman-results.json) |
| Recurso remoto inexistente | Mascota o veterinario inexistentes devuelven 404; no se guarda un turno adicional | [Resultados Newman](sprint-11-openfeign.newman-results.json) |
| Servicio remoto detenido | Mascotas y Veterinarios se detienen por separado; Turnos devuelve 503 sin persistir | [Indisponibilidad y recuperación](sprint-11-indisponibilidad-timeout.json) |
| Timeout real | Contenedor de Mascotas pausado: Turnos devuelve 503 cerca del readTimeout de 5000 ms y no guarda | [Medición del timeout](sprint-11-indisponibilidad-timeout.json) |
| Recuperación | Tras restaurar cada servicio se vuelve a crear un turno con 201; todos los registros de prueba se eliminan | [Resultados de fallos](sprint-11-indisponibilidad-timeout.json) |
| Arquitectura | Aplicación y dominio sin imports Feign ni adaptadores; DTOs de consumo propios; clientes por nombre, sin URL fija | [Resumen del proyecto](sprint-11-verificaciones.json) |
| Configuración | Timeouts 3000/5000 ms, compresión habilitada y logging BASIC/DEBUG desde patitas-config | [Resultados Newman](sprint-11-openfeign.newman-results.json) |

Las 43 pruebas incluyen las 15 existentes de Dueños, ocho casos de uso de Turnos y diez verificaciones por adaptador Feign. Se comprueban el estado PENDIENTE, el orden de validación, la ausencia de escritura tras errores, la conservación de los datos al rechazar actualizaciones, la cancelación sin dependencia remota y la traducción exclusiva de HTTP 404 a false. Los HTTP 400, 401, 403, 500, 502 y 503 se consideran indisponibilidad y conservan la causa; también se rechazan respuestas vacías o de otro ID.

La colección usa identificadores generados y crea sus propios datos. Debe ejecutarse con las cuatro APIs disponibles y sin registros previos:

```powershell
npx newman run docs/evidencias/sprint-11/sprint-11-openfeign.postman_collection.json
```

La prueba adicional de fallos usa los contenedores reales: primero stop/start de Mascotas y Veterinarios, y luego pause/unpause de Mascotas para producir una espera de lectura. Se verifican HTTP 503, ausencia de persistencia y recuperación con HTTP 201. No se agregan demoras artificiales ni endpoints de prueba a las aplicaciones.

La configuración está publicada en [patitas-config](https://github.com/ggomarighetti/patitas-config). Se usa el prefijo vigente spring.cloud.openfeign del [manual oficial](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html). Se mantiene el paquete com.patitas.turno.adapter.out.feign del proyecto, las excepciones reutilizables por categoría y el manejo HTTP en config/GlobalExceptionHandler.java.

La consigna del sprint 11 prevalece sobre la vista previa del sprint 10: este sprint implementa OpenFeign. API Gateway corresponde al sprint 12 y Circuit Breaker al sprint 13.
