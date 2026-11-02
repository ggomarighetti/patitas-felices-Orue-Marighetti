# Documentos de los sprints

Se conserva la consigna original sin modificar el PDF.

| Sprint | Consigna | Alcance desarrollado |
| --- | --- | --- |
| 09 | [Arquitectura hexagonal y descomposición en microservicios](Sprint_09_v2.pdf) | Pasos 1 a 8, ms-dueno funcional |
| 10 | [Eureka, Config Server y Actuator](Sprint_10_Eureka_ConfigServer_Actuator.pdf) | Infraestructura funcional y estructura de los otros contextos |

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

Las verificaciones y la captura del dashboard de Eureka están en las [evidencias del sprint 10](../evidencias/sprint-10/README.md). Gateway, Circuit Breaker y comunicación HTTP entre contextos quedan para sus consignas posteriores.
