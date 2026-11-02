# Documentos de los sprints

Se conserva la consigna original sin modificar el PDF.

| Sprint | Consigna | Alcance desarrollado |
| --- | --- | --- |
| 09 | [Arquitectura hexagonal y descomposición en microservicios](Sprint_09_v2.pdf) | Pasos 1 a 8, ms-dueno funcional |

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

Los módulos de mascotas, veterinarios y turnos quedan reservados. No se implementan entregables del sprint 10.
