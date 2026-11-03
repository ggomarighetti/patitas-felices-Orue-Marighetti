# Documentos de los sprints

Se conserva la consigna original sin modificar el PDF.

| Sprint | Consigna | Alcance desarrollado |
| --- | --- | --- |
| 09 | [Arquitectura hexagonal y descomposición en microservicios](Sprint_09_v2.pdf) | Pasos 1 a 8, ms-dueno funcional |
| 10 | [Eureka, Config Server y Actuator](Sprint_10_Eureka_ConfigServer_Actuator.pdf) | Infraestructura funcional y estructura de los otros contextos |
| 11 | [Comunicación con OpenFeign](Sprint_11_OpenFeign_Comunicacion.pdf) | APIs de Mascotas, Veterinarios y Turnos; validación cruzada, errores y timeout |
| 12 | [API Gateway](Sprint_12_APIGateway.pdf) | Entrada reactiva en 8080, rutas con Eureka, CORS, logging, Actuator y configuración Git |

## Sprint 09

| Paso | Implementación |
| --- | --- |
| 1. Proyecto padre Maven | Cuatro módulos y gestión de dependencias comunes |
| 2. Módulo ms-dueno | Paquetes hexagonales, MySQL dueno_db y puerto 8081 |
| 3. Dominio | Dueno, puertos de entrada y salida y excepciones reutilizables |
| 4. Aplicación | Casos de uso, email único en alta y actualización y control de ausencia |
| 5. Persistencia | Adaptador del puerto de salida, Spring Data JPA y restricción única de email |
| 6. Web | DTOs validados, mapper MapStruct, CRUD REST y errores centralizados |
| 7. Ejecución | Clase principal, JAR ejecutable, MySQL y Swagger |
| 8. Arquitectura | Revisión de imports y decisiones documentadas en el README |

El [README](../../README.md#arquitectura) explica las decisiones necesarias para resolver las contradicciones de la guía: entidad JPA en el dominio y composición del adaptador de persistencia. AGENTS.md exige excepciones por categoría; se usa DuplicateResourceException con mensaje recibido al lanzarla, conservando la causa cuando se traduce un error.

Las verificaciones realizadas y la demostración funcional se conservan en [evidencias del sprint 09](../evidencias/sprint-09/README.md).

El alcance anterior corresponde a la entrega del sprint 09. Su configuración y sus DTOs se adaptan en el sprint 10.

## Sprint 10

Se incorporan eureka-server (8761), config-server (8888), el repositorio público patitas-config y Actuator en ms-dueno. Los DTOs de dueños usan Lombok y el mapper es manual y estático. La API conserva sus validaciones, conflictos y respuestas de error centralizadas.

Mascota, Veterinario y Turno tienen las entidades y puertos definidos en la sección 4, junto con sus directorios de aplicación y adaptadores. Son módulos compilables, todavía sin servidores REST; el criterio de aceptación permite estructuras vacías.

Se conserva Spring Boot 4.1.0 y se usa Spring Cloud 2025.1.4 por compatibilidad. El cliente exige Config Server al arrancar. Las diferencias respecto de los ejemplos de la consigna se explican en el [README](../../README.md#configuración-centralizada-y-actuator).

Las verificaciones y la captura del dashboard de Eureka están en las [evidencias del sprint 10](../evidencias/sprint-10/README.md). Esa entrega conserva su alcance histórico; los módulos se completan en el sprint 11.

## Sprint 11

Se completan los servicios REST de Mascotas, Veterinarios y Turnos para ejecutar el flujo de la nueva consigna. Turnos usa OpenFeign y Spring Cloud LoadBalancer con Eureka para verificar la mascota y el veterinario antes de guardar un turno PENDIENTE. Los puertos de dominio abstraen la comunicación y los DTOs de consumo pertenecen exclusivamente a ms-turno.

Los recursos remotos inexistentes devuelven 404; las caídas y timeouts devuelven 503 sin persistir. La configuración centralizada establece conexión de 3000 ms y lectura de 5000 ms con el prefijo actual spring.cloud.openfeign. Se mantiene la convención de paquetes y las excepciones por categoría del proyecto.

Las [evidencias del sprint 11](../evidencias/sprint-11/README.md) contienen la colección, sus resultados, las pruebas reales de stop/start y pause/unpause, y la captura de Eureka con los cuatro servicios. Gateway y Circuit Breaker corresponden a los sprints 12 y 13 según este nuevo PDF.

## Sprint 12

Se agrega api-gateway como módulo del reactor Maven, usando WebFlux y Netty. Las cuatro rutas canónicas de negocio reciben solicitudes en 8080 y usan destinos lb:// resueltos por Eureka. Discovery Locator agrega cuatro prefijos automáticos y el paso opcional RewritePath agrega /v1/duenos: nueve rutas en total, sin una ruta del gateway hacia sí mismo.

El gateway se registra como quinta aplicación en Eureka, obtiene su configuración desde patitas-config/api-gateway.yml y expone health, info y la inspección de rutas de Actuator en modo de solo lectura. Se implementan CORS de desarrollo sin credenciales y LoggingFilter con orden -1, estado HTTP efectivo y duración. Los puertos de negocio 8081 a 8084 dejan de publicarse en Docker; las cabeceras Location apuntan al acceso público.

Las [evidencias del sprint 12](../evidencias/sprint-12/README.md) conservan la colección de 66 solicitudes y 131 verificaciones, el resultado de las 47 pruebas Maven, el registro de las cinco aplicaciones y el balanceo entre dos instancias reales de Dueños. Las diferencias de nombres de propiedades y dependencias respecto del ejemplo se explican en el [README](../../README.md#api-gateway--sprint-12), usando la versión compatible con Boot 4.1.0.

Se mantienen las bases MySQL de los servicios. Circuit Breaker queda para el sprint 13 y JWT/rate limiting para el 14, conforme a la consigna activa.
