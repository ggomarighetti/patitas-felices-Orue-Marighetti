# Evidencias del sprint 09

Verificación realizada el 2 de noviembre de 2026. Compilación con Temurin 21.0.10 y Maven Wrapper 3.9.16; ejecución del mismo JAR en Temurin 25 sobre Linux, con MySQL 8.0.40 mediante Docker Compose.

| Verificación | Resultado | Evidencia |
| --- | --- | --- |
| Maven validate desde la raíz | Padre y cuatro módulos válidos | [Resumen](sprint-09-verificaciones.json) |
| Maven clean package -pl ms-dueno | Compilación y JAR ejecutable correctos | [Resumen](sprint-09-verificaciones.json) |
| Maven package -pl ms-dueno tras completar Swagger | 15 pruebas aprobadas, 0 fallos, errores u omisiones | [Resumen](sprint-09-verificaciones.json) |
| API y documentación | 40 solicitudes y 81 verificaciones aprobadas, 0 fallos | [Resultados de Newman](sprint-09-duenos.newman-results.json) |
| Ocho altas simultáneas con el mismo email | Una respuesta 201, siete respuestas 409 y un único registro persistido | [Resultados de concurrencia](sprint-09-concurrencia.json) |
| Persistencia | Tabla duenos y restricción uk_duenos_email creadas por Hibernate en dueno_db | [Esquema MySQL](sprint-09-verificaciones.json) |
| Arquitectura y reglas del proyecto | Imports revisados, controlador dependiente del puerto de entrada, código Java sin comentarios | [Resumen](sprint-09-verificaciones.json) |
| Consigna archivada | SHA-256 idéntico al PDF cargado en VetSystem | [Resumen](sprint-09-verificaciones.json) |
| POST desde Swagger | Dueño registrado con HTTP 201 | [Captura](sprint-09-swagger-post.png) |
| GET desde Swagger | HTTP 200 y dueño registrado presente en la lista | [Captura](sprint-09-swagger-get.png) |

La [colección de Postman](sprint-09-duenos.postman_collection.json) crea dos dueños y verifica consultas, altas, actualizaciones y bajas. Comprueba emails duplicados con distintas mayúsculas, conservar el email propio al actualizar, rechazar datos inválidos sin modificar lo almacenado, recursos inexistentes, identificadores inválidos, JSON malformado, métodos y tipos de contenido incorrectos, Swagger y OpenAPI. Elimina sus datos al finalizar. El resumen JSON conserva las respuestas y cada verificación; es un formato propio derivado del reporte de Newman.

La prueba concurrente se ejecutó con ocho solicitudes POST sincronizadas mediante una barrera. El registro ganador se consultó para comprobar que solo existía uno y se eliminó al terminar. El log de Hibernate confirmó conflictos de la restricción uk_duenos_email (MySQL 1062), por lo que se ejercitó también su traducción a HTTP 409.

La demostración de Swagger utiliza datos ficticios y su registro también se elimina al terminar. Para reproducir la colección, iniciar el servicio según el [README principal](../../../README.md) y ejecutarla sobre una base sin dueños:

```powershell
npx newman run docs/evidencias/sprint-09/sprint-09-duenos.postman_collection.json
```

El arranque nativo en Windows se intentó con los JDK instalados y falló en una conexión interna de java.nio (`Unable to establish loopback connection`). La ejecución funcional verificada utiliza el perfil app de Docker Compose. Los módulos reservados no tienen implementación y no se incluyen componentes del sprint 10.
