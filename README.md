# Patitas Felices

Sistema de gestión veterinaria de la materia Microservicios y APIs Escalables de la Universidad de Palermo. Integrante: Guillermo Gabriel Orue Marighetti.

Este repositorio continúa la Fase 2 del trabajo práctico. El [monolito VetSystem, sprints 01 a 08](https://github.com/ggomarighetti/vetSystem-Orue-Marighetti) conserva sus entregas e historial. Se toma como base el [avance de los pasos 1 a 4 del sprint 09](https://github.com/ggomarighetti/patitas-felices-Orue-Marighetti/tree/e5cdd7d), cuyo historial también se conserva aquí.

**Sprint 09:** proyecto Maven multi-módulo y `ms-dueno` con API REST, MySQL, validaciones y Swagger. Se incluyen la [consigna original](docs/sprints/Sprint_09_v2.pdf), el [seguimiento](docs/sprints/README.md) y las [evidencias](docs/evidencias/sprint-09/README.md).

## Requisitos

- JDK 21, con `JAVA_HOME` apuntando al JDK. El wrapper incluido utiliza Maven 3.9.16.
- MySQL 8 con la base `dueno_db` creada previamente, o Docker Desktop para levantar la base incluida.
- Acceso a Maven Central en la primera compilación.

En PowerShell, si el JDK instalado no está seleccionado:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE/.jdks/temurin-21.0.10"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
.\mvnw.cmd -version
```

## Ejecución

### Aplicación y MySQL con Docker

Desde la raíz, compilar y levantar el servicio junto con su base:

```powershell
Copy-Item .env.example .env
.\mvnw.cmd clean package -pl ms-dueno
docker compose --profile app up -d --wait
```

Swagger estará disponible en <http://localhost:8081/swagger-ui/index.html> cuando finalice el arranque de Spring. El perfil `app` ejecuta el JAR compilado para Java 21 en Temurin 25 sobre Linux y lo conecta a MySQL por la red interna de Compose. Después de recompilar, reiniciar la aplicación con `docker compose --profile app restart ms-dueno`.

Esta es la modalidad verificada en este equipo: el arranque directo con los JDK de Windows instalados falla en la conexión interna de `java.nio` con `Unable to establish loopback connection`. Docker permite ejecutar el mismo JAR sin modificar la configuración del sistema.

El contenedor crea `dueno_db`, con usuario `patitas` y contraseña de desarrollo `patitas-local`. `.env` queda excluido de Git. Para usar otra contraseña, cambiar `DB_PASSWORD` en `.env` antes de la primera ejecución. El volumen conserva los datos al detener el contenedor; las variables de inicialización se aplican al crear el volumen.

### Aplicación con Java local

En un entorno con Java local funcionando, se puede ejecutar solo MySQL con `docker compose up -d --wait`.

Exportar al proceso de Spring las mismas credenciales definidas en `.env` y ejecutar el servicio:

```powershell
$env:DB_USERNAME = 'patitas'
$env:DB_PASSWORD = 'patitas-local'
.\mvnw.cmd spring-boot:run -pl ms-dueno
```

Spring Boot no carga `.env` automáticamente; Docker Compose sí lo utiliza. Si se usa MySQL instalado, crear `dueno_db` en Workbench y configurar las variables con el usuario correspondiente. Hibernate crea la tabla `duenos` al arrancar.

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/dueno_db` | Conexión del servicio |
| `DB_USERNAME` | `patitas` | Usuario MySQL |
| `DB_PASSWORD` | Vacío | Contraseña MySQL |
| `MYSQL_PORT` | `3306` | Puerto publicado por Docker Compose |

Si se cambia `MYSQL_PORT`, configurar `DB_URL` con ese mismo puerto. No se configura `spring.jpa.database-platform`: Hibernate detecta el dialecto.

- Swagger: <http://localhost:8081/swagger-ui/index.html>.
- OpenAPI: <http://localhost:8081/v3/api-docs>.
- API: <http://localhost:8081/api/duenos>.

Para detener la aplicación iniciada en la terminal, usar `Ctrl+C`. Para detener ambos contenedores conservando los datos:

```powershell
docker compose --profile app stop
```

## API de dueños

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/duenos` | 200, lista de dueños |
| GET | `/api/duenos/{id}` | 200, dueño; 404 si no existe |
| POST | `/api/duenos` | 201 y cabecera `Location` |
| PUT | `/api/duenos/{id}` | 200, dueño actualizado; 404 si no existe |
| DELETE | `/api/duenos/{id}` | 204; 404 si no existe |

POST y PUT reciben los mismos campos:

```json
{
  "nombre": "Ana",
  "apellido": "Gómez",
  "email": "ana@example.com",
  "telefono": "1112345678"
}
```

Los cuatro campos son obligatorios. Nombre y apellido admiten hasta 100 caracteres, email hasta 254 y teléfono hasta 30. El email debe ser válido y se almacena en minúsculas. El sprint 09 define estos campos, por eso este contrato no incluye el DNI del monolito.

La respuesta agrega `id`. La creación asigna el identificador en el servidor; la actualización conserva el de la ruta. Los datos inválidos y los identificadores no positivos responden 400. El email repetido responde 409 tanto al crear como al actualizar; el mismo dueño puede conservar su email. La restricción `uk_duenos_email` protege también las escrituras concurrentes. Al traducir un conflicto de persistencia se conserva su causa original.

Los errores usan `timestamp`, `status`, `error`, `mensaje` y `path`. La conversión a HTTP se centraliza en `config/GlobalExceptionHandler.java`. Se reutilizan excepciones por categoría (`DuplicateResourceException`, `ResourceNotFoundException`), de acuerdo con `AGENTS.md`.

## Arquitectura

```text
patitas-felices/
  ms-dueno/
    src/main/java/com/patitas/dueno/
      domain/
        model/                  Dueno
        exception/              Excepciones por categoría
        port/in/                DuenoService
        port/out/               DuenoRepository
      application/              DuenoServiceImpl
      adapter/in/web/           DuenoController y DuenoWebMapper
        dto/                    DuenoRequest, DuenoResponse y ErrorResponse
      adapter/out/persistence/  DuenoJpaRepository y DuenoPersistenceAdapter
      config/                   GlobalExceptionHandler
      MsDuenoApplication.java
    src/main/resources/application.yml
    src/test/java/              Pruebas de casos de uso
    pom.xml
  ms-mascota/pom.xml
  ms-veterinario/pom.xml
  ms-turno/pom.xml
  docs/
  compose.yml
  pom.xml
```

`DuenoController` usa el puerto `DuenoService` porque necesita el contrato de los casos de uso. Esto permite sustituir `DuenoServiceImpl` y probar el controlador con otra implementación. El controlador recibe y devuelve DTOs; el mapper de MapStruct convierte los modelos sin importar `domain.model` en el controlador.

`DuenoServiceImpl` recibe `DuenoRepository` por constructor y controla la unicidad de email. No importa adaptadores, JPA ni Spring Web. Las transacciones agrupan las consultas y escrituras. El adaptador de persistencia implementa ese puerto y delega en `DuenoJpaRepository`, que extiende Spring Data JPA. La composición resuelve `update(Long, Dueno)`, método que `JpaRepository` no proporciona, y evita mezclar su contrato genérico de `save` con el puerto propio.

La consigna combina un dominio independiente de frameworks con la exigencia explícita de `@Entity` y Lombok sobre `Dueno`. Se mantiene esa simplificación didáctica del paso 3: el modelo depende de Jakarta Persistence y Lombok; los puertos y excepciones son Java puro. No hay Spring ni referencias a adaptadores en el dominio. Esta dependencia de JPA impide considerarlo completamente independiente de persistencia.

### Contextos y alcance

| Módulo | Puerto | Responsabilidad | Estado en sprint 09 |
| --- | --- | --- | --- |
| `ms-dueno` | 8081 | CRUD de dueños y email único | Implementado |
| `ms-mascota` | 8082 | Mascotas y referencia al dueño | POM reservado |
| `ms-veterinario` | 8083 | Veterinarios y especialidades | POM reservado |
| `ms-turno` | 8084 | Turnos y solapamiento | POM reservado |

Cada servicio será dueño de sus datos y de su base; no habrá JOIN ni relaciones JPA entre servicios. Se comunicarán mediante APIs o eventos. Los otros módulos usan `packaging=pom` para que el reactor sea válido. Su implementación, Eureka, Config Server y Actuator pertenecen al sprint 10.

Los microservicios permiten desplegar y escalar por contexto y aislar fallos de proceso, pero agregan comunicación por red, configuración y observabilidad. Para el tamaño actual de la clínica el monolito sigue siendo una alternativa válida; esta separación responde al objetivo didáctico del trabajo práctico.

## Verificaciones

```powershell
.\mvnw.cmd validate
.\mvnw.cmd clean package -pl ms-dueno
```

Las pruebas de aplicación usan un repositorio en memoria como doble, sin levantar Spring ni MySQL. La [colección de Postman](docs/evidencias/sprint-09/sprint-09-duenos.postman_collection.json) verifica la API contra MySQL. Las [evidencias](docs/evidencias/sprint-09/README.md) indican las ejecuciones realizadas y sus resultados.

Con MySQL y el servicio iniciados, ejecutar la colección sobre una base sin dueños, en el orden guardado:

```powershell
npx newman run docs/evidencias/sprint-09/sprint-09-duenos.postman_collection.json
```

La colección crea y elimina sus propios datos. Para ejecutar el JAR generado:

```powershell
java -jar ms-dueno/target/ms-dueno-0.0.1-SNAPSHOT.jar
```
