# Evidencias del sprint 13

Verificación realizada el 2 de noviembre de 2026 (Argentina). Java 21 y Maven 3.9.16 para compilar; Temurin 25 en Linux, Spring Boot 4.1.0, Spring Cloud 2025.1.4 y Resilience4j 2.4.0 para ejecutar. El entorno de Compose contiene cuatro bases MySQL, Redis, los cuatro microservicios, Eureka, Config Server y Gateway. Los JSON conservan tiempos UTC.

| Verificación | Resultado | Evidencia |
| --- | --- | --- |
| Compilación completa | Padre y siete módulos; 55 pruebas aprobadas sin fallos, errores ni omisiones | [Verificaciones del proyecto](sprint-13-verificaciones.json) |
| Flujo funcional | 75 solicitudes y 151 verificaciones aprobadas; CRUD, OpenFeign, CORS, rutas, Redis y Actuator | [Colección](sprint-13-resiliencia.postman_collection.json) y [resultados Newman](sprint-13-resiliencia.newman-results.json) |
| Circuitos CLOSED | Turno creado con 201/PENDIENTE y referencias válidas; ambos circuitos CLOSED | [Escenario normal](sprint-13-resiliencia.json) |
| Caída de Mascotas | Reintentos, fallback 503, circuito OPEN y ausencia de escrituras | [Escenarios reales](sprint-13-resiliencia.json) |
| Caída de Veterinarios | Mismo comportamiento con circuito independiente y mensaje del servicio correspondiente | [Escenarios reales](sprint-13-resiliencia.json) |
| OPEN inmediato | Tres peticiones adicionales por circuito rechazadas con 503 en menos de un segundo, sin nuevos reintentos | [Tiempos y eventos](sprint-13-resiliencia.json) |
| Recuperación | Transición automática tras 10 segundos a HALF_OPEN; servicio restaurado y tres llamadas 201; cierre a CLOSED | [Salud y transiciones](sprint-13-resiliencia.json) |
| Rate Limiter | Ráfaga concurrente de 60 GET mezclando ruta canónica y alias; respuestas 200 y 429, una cuota compartida en Redis | [Ráfaga y claves](sprint-13-rate-limiter.json) |
| Rechazos sin llegar al backend | El incremento de GET 200 en la métrica de Turnos coincide exactamente con las solicitudes aceptadas por Gateway | [Contadores de ambos lados](sprint-13-rate-limiter.json) |
| Regeneración de tokens | Después de la ráfaga se vuelve a obtener 200 | [Recuperación de la cuota](sprint-13-rate-limiter.json) |
| Limpieza y operación | Datos de prueba eliminados, circuitos CLOSED, cinco aplicaciones UP y doce contenedores saludables | [Verificación final](sprint-13-verificaciones.json) |

Las 55 pruebas incluyen las 47 existentes, seis pruebas de los adaptadores a través de proxies Spring AOP reales y dos de la clave de Rate Limiter. Se comprueban tres intentos ante fallos transitorios, un solo resultado lógico para Circuit Breaker, conservación de la causa original, aislamiento de circuitos, rechazo inmediato en OPEN, cierre tras tres llamadas correctas y ausencia de reintentos de 404 y de un error 403. El resolver usa la dirección remota e ignora los valores de cabeceras y consulta.

Para repetir la colección, iniciar el ecosistema según el [README](../../../README.md), esperar que las cuatro listas públicas respondan 200 y usar las bases vacías con los circuitos CLOSED:

```powershell
npx newman run docs/evidencias/sprint-13/sprint-13-resiliencia.postman_collection.json --delay-request 120
```

La demora mantiene la prueba funcional dentro de la cuota. El control de Retry considera solo los eventos producidos durante esa ejecución, para permitir historial de pruebas anteriores. Las consultas a 8761 y 8888 son administrativas; las cuatro APIs y Actuator de Turnos se consultan mediante Gateway en 8080. La colección genera sus identificadores y elimina sus datos.

Para repetir las caídas y la ráfaga con el ecosistema completo de Compose activo, las bases vacías y los circuitos CLOSED:

```powershell
python docs/evidencias/sprint-13/verificar-resiliencia.py
```

El [script de verificación](verificar-resiliencia.py) crea dueño, mascota, veterinario y turno propios. Detiene Mascotas y Veterinarios por separado. Después de cada 503 compara la lista de turnos con su valor previo. Comprueba OPEN en el detalle de salud y exige que las llamadas bloqueadas no generen nuevos eventos de Retry. Luego observa HALF_OPEN sin enviar llamadas de negocio, restaura la dependencia y espera que el cliente Eureka de Turnos vuelva a registrarla y que venza la caché de LoadBalancer. Solo entonces realiza las tres llamadas de prueba y comprueba HALF_OPEN, HALF_OPEN y CLOSED. El bloque de limpieza restaura los servicios y elimina únicamente sus propios registros.

El mínimo de cinco llamadas cuenta tanto éxitos como fallos. Por ejemplo, un éxito y cuatro fallos ya superan el mínimo y el umbral de 50%; no se confunde ese parámetro con cinco errores consecutivos. Retry tiene tres intentos totales y dos esperas: 1 y 2 segundos. Sus eventos y los tiempos de cada solicitud quedan archivados.

En Resilience4j 2.4.0, el detalle de salud informa CIRCUIT_OPEN o CIRCUIT_HALF_OPEN. El agregador general puede conservar UP: las verificaciones examinan el estado del circuito. Readiness verifica la aplicación y su base para que el fallback siga disponible mientras falla otra API.

La ráfaga usa exclusivamente GET, después de eliminar los datos. Consulta el contador `http.server.requests` del backend antes y después: su diferencia debe ser igual al número de respuestas 200, demostrando que los 429 no llegaron a Turnos. Se inspecciona un único par de claves de tokens/timestamp para la ruta compartida. El prefijo `/ms-turno` se integra en la ruta canónica; quedan ocho rutas de Gateway y se conserva la funcionalidad del alias.

La [configuración de resiliencia](https://github.com/ggomarighetti/patitas-config/blob/c369260a51fec773c2f1b7165333652ad306744b/ms-turno.yml) y la [configuración de Gateway/Redis](https://github.com/ggomarighetti/patitas-config/blob/c369260a51fec773c2f1b7165333652ad306744b/api-gateway.yml) están publicadas. Los bootstraps locales siguen siendo mínimos. Se conserva RemoteServiceUnavailableException como excepción reutilizable para el fallback, con causa, y su mapeo 503 centralizado.

Se conserva sin modificar la [consigna original](../../sprints/Sprint_13_Resilience4j.pdf). Las adaptaciones a Boot 4 y los nombres actuales de propiedades se explican en el [README](../../../README.md#resiliencia--sprint-13), con referencias oficiales. JWT queda fuera de esta entrega.
