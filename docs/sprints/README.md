# Sprints

Se conserva cada consigna original en esta carpeta, sin modificar el PDF.
Las nuevas versiones se guardarán con nombres distintos para mantener el historial.

| Sprint | Consigna | Alcance desarrollado |
| --- | --- | --- |
| 9 | [Sprint_09_v2.pdf](Sprint_09_v2.pdf) | Pasos 1 a 4 de la sección 6 |

## Sprint 9: arquitectura hexagonal y descomposición en microservicios

La solicitud actual llega hasta **PASO 4 - Implementar la capa Application**, inclusive.
Los criterios de aceptación del PDF corresponden al sprint completo; el servidor, Swagger y el CRUD
HTTP aún no forman parte de esta entrega parcial.

| Paso | Estado | Evidencia |
| --- | --- | --- |
| 1. Proyecto padre Maven | Implementado | POM con cuatro módulos y dependencias comunes; Maven Wrapper |
| 2. Módulo ms-dueno | Implementado | POM hijo, paquetes completos y configuración de `dueno_db` / puerto 8081 |
| 3. Domain | Implementado | `Dueno`, `DuenoService`, `DuenoRepository` y excepción de email duplicado |
| 4. Application | Implementado | `DuenoServiceImpl`, inyección por constructor, delegación y validación en `save` |
| 5. Adaptador de persistencia | Pendiente | Implementar el puerto de salida con JPA y MySQL |
| 6. Adaptador web | Pendiente | DTOs, validaciones, controlador y respuestas de error |
| 7. Primera ejecución | Pendiente | Clase principal, JAR ejecutable, MySQL y comprobación en Swagger |
| 8. Verificación completa de arquitectura | Pendiente | Revisar también los adaptadores cuando existan |

### Consideraciones al continuar

La contradicción entre dominio puro y entidad JPA, los contratos elegidos y la configuración están
explicados en el [README principal](../../README.md#arquitectura).

En el paso 5, `JpaRepository` no aporta por sí mismo `update(Long, Dueno)` y su `save` es genérico.
Conviene usar un adaptador que implemente `DuenoRepository` y delegue en una interfaz Spring Data,
resolviendo explícitamente actualización, ausencia y unicidad de email. Esto conserva las dependencias
hacia los puertos y evita asumir que extender `JpaRepository` implementa automáticamente todo el contrato.

### Verificación de esta entrega

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd validate
.\mvnw.cmd clean verify
.\mvnw.cmd clean package -pl ms-dueno
```

Las pruebas están en `ms-dueno/src/test/java/com/patitas/dueno/application/DuenoServiceImplTest.java`.
La revisión de imports debe mantener el dominio sin Spring y la aplicación sin `adapter.*`,
`jakarta.persistence`, `javax.persistence` ni Spring Web.

Resultado comprobado el 29 de septiembre de 2026 con JDK 25 (compilación dirigida a Java 21)
y Maven 3.9.16:

- `validate`: correcto para el padre y los cuatro módulos.
- `clean verify`: correcto para todo el reactor; JAR de `ms-dueno` generado.
- 10 pruebas ejecutadas, sin fallos, errores ni omisiones.
- Imports de dominio y aplicación revisados, sin dependencias prohibidas.
- PDF archivado verificado mediante SHA-256: idéntico al adjunto original.
