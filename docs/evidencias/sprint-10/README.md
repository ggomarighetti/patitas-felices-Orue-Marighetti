# Evidencias del sprint 10

Verificación realizada el 2 de noviembre de 2026. Se usa Spring Boot 4.1.0, Spring Cloud 2025.1.4, compilación Java 21 y ejecución en Temurin 25 sobre Linux mediante Docker Compose, con MySQL 8.0.40.

La colección ejecutó 55 solicitudes y 111 verificaciones, sin fallos. Las 15 pruebas de aplicación también finalizaron sin fallos, errores u omisiones.

| Criterio | Verificación | Evidencia |
| --- | --- | --- |
| Compilación desde la raíz | Padre y seis módulos compilados; 15 pruebas aprobadas | [Resumen del proyecto](sprint-10-verificaciones.json) |
| Eureka en 8761 | Dashboard disponible y MS-DUENO registrado como UP en puerto 8081 | [Captura del dashboard](sprint-10-eureka-ms-dueno.png) y [resultados HTTP](sprint-10-infraestructura-duenos.newman-results.json) |
| Config Server en 8888 | Configuración propia y compartida desde Git para los cuatro servicios; respuesta JSON Environment y representación YAML | [Resultados HTTP](sprint-10-infraestructura-duenos.newman-results.json) |
| Configuración efectiva de ms-dueno | Health muestra Config Server y MySQL UP; env muestra fuentes configserver y oculta sus valores | [Resultados HTTP](sprint-10-infraestructura-duenos.newman-results.json) |
| Actuator | health, info, metrics, env, beans y mappings responden correctamente | [Resultados HTTP](sprint-10-infraestructura-duenos.newman-results.json) |
| CRUD y Swagger | Altas, consultas, actualizaciones, bajas, validaciones, conflictos y OpenAPI comprobados | [Colección Postman](sprint-10-infraestructura-duenos.postman_collection.json) y [resultados HTTP](sprint-10-infraestructura-duenos.newman-results.json) |
| Orden de arranque | Config Server antes de Eureka y ms-dueno; arranque de prueba sin Config Server termina con ConfigClientFailFastException y salida 1 | [Resumen del proyecto](sprint-10-verificaciones.json) |
| Dependency Rule | Aplicación sin imports de adaptadores, JPA ni Spring Web; controlador depende del puerto de entrada | [Resumen del proyecto](sprint-10-verificaciones.json) |
| Otros contextos | Modelos y puertos de Mascota, Veterinario y Turno compilados; capas externas reservadas; referencias Long sin relaciones entre servicios | [Resumen del proyecto](sprint-10-verificaciones.json) |

La colección combina las verificaciones de infraestructura con la regresión de dueños. Requiere una base sin dueños y elimina sus propios registros al finalizar:

```powershell
npx newman run docs/evidencias/sprint-10/sprint-10-infraestructura-duenos.postman_collection.json
```

El resumen JSON conserva los resultados de cada solicitud y verificación. Para las respuestas extensas de env, beans y mappings conserva un resumen de sus estructuras en lugar del cuerpo completo; las verificaciones se ejecutan sobre las respuestas originales.

Las instrucciones reproducibles están en el [README principal](../../../README.md). La configuración pública está en [patitas-config](https://github.com/ggomarighetti/patitas-config); sus contraseñas son placeholders de variables de entorno.

La guía propone Cloud 2025.0.0, incompatible con Boot 4.1. Se conserva Boot 4.1.0 y se usa Cloud 2025.1.4 de acuerdo con la [matriz oficial](https://spring.io/projects/spring-cloud/). El cliente usa un import obligatorio de Config Server para cumplir el orden de arranque exigido. Las excepciones siguen siendo reutilizables y el manejo HTTP permanece en config/GlobalExceptionHandler.java según AGENTS.md.
