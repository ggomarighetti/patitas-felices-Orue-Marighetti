# Patitas Felices

Sistema de gestión veterinaria de la materia Microservicios y APIs Escalables de la Universidad de Palermo. Integrante: Guillermo Gabriel Orue Marighetti.

Este repositorio continúa la Fase 2 del trabajo práctico. El [monolito VetSystem, sprints 01 a 08](https://github.com/ggomarighetti/vetSystem-Orue-Marighetti) conserva sus entregas e historial. Se toma como base el [avance de los pasos 1 a 4 del sprint 09](https://github.com/ggomarighetti/patitas-felices-base-sprint-09/tree/e5cdd7d), cuyo historial también se conserva aquí.

**Sprint 10:** infraestructura Eureka y Config Server, configuración centralizada en [patitas-config](https://github.com/ggomarighetti/patitas-config), Actuator y ms-dueno registrado. Se incluyen la [consigna del sprint 10](docs/sprints/Sprint_10_Eureka_ConfigServer_Actuator.pdf) y sus [evidencias](docs/evidencias/sprint-10/README.md).

**Sprint 09:** proyecto Maven multi-módulo y `ms-dueno` con API REST, MySQL, validaciones y Swagger. Se incluyen la [consigna original](docs/sprints/Sprint_09_v2.pdf), el [seguimiento](docs/sprints/README.md) y las [evidencias](docs/evidencias/sprint-09/README.md).

## Requisitos

- JDK 21, con `JAVA_HOME` apuntando al JDK. El wrapper incluido utiliza Maven 3.9.16.
- MySQL 8 con la base `dueno_db` creada previamente, o Docker Desktop para levantar la base incluida.
- Acceso a Maven Central en la primera compilación y a GitHub para que Config Server clone el repositorio público de configuración.

En PowerShell, si el JDK instalado no está seleccionado:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE/.jdks/temurin-21.0.10"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
.\mvnw.cmd -version
```

## Ejecución

### Ecosistema con Docker

Desde la raíz, compilar todos los módulos y levantar el ecosistema:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
.\mvnw.cmd clean package
docker compose --profile app up -d --wait
```

Compose espera que Config Server esté disponible antes de iniciar Eureka y que ambos estén disponibles antes de iniciar ms-dueno. MySQL arranca como dependencia del servicio. Los tres JARs se compilan para Java 21 y se ejecutan en Temurin 25 sobre Linux.

1. Config Server en 8888 clona la rama main de patitas-config.
2. Eureka Server en 8761 habilita el registro.
3. ms-dueno obtiene su configuración remota, conecta a MySQL y se registra en Eureka.

Después de recompilar un servicio, reiniciar su contenedor. `docker compose --profile app stop` conserva el volumen de MySQL.

Esta es la modalidad verificada en este equipo: el arranque directo con los JDK de Windows instalados falla en la conexión interna de `java.nio` con `Unable to establish loopback connection`. Docker permite ejecutar el mismo JAR sin modificar la configuración del sistema.

El contenedor crea `dueno_db`, con usuario `patitas` y contraseña de desarrollo `patitas-local`. `.env` queda excluido de Git. Para usar otra contraseña, cambiar `DB_PASSWORD` en `.env` antes de la primera ejecución. El volumen conserva los datos al detener el contenedor; las variables de inicialización se aplican al crear el volumen.

### Orden de arranque con Java local

En un entorno con Java local funcionando, se puede ejecutar solo MySQL con `docker compose up -d --wait`. Después, desde la raíz, iniciar cada aplicación en una terminal distinta y respetar el orden:

```powershell
.\mvnw.cmd spring-boot:run -pl config-server
```

Esperar que <http://localhost:8888/actuator/health> indique UP y luego iniciar:

```powershell
.\mvnw.cmd spring-boot:run -pl eureka-server
```

Esperar que <http://localhost:8761/actuator/health> indique UP y recién entonces iniciar ms-dueno.

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
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Dirección de arranque del cliente |
| `CONFIG_GIT_URI` | `https://github.com/ggomarighetti/patitas-config.git` | Repositorio que clona Config Server |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Registro obtenido desde configuración compartida |

Si se cambia `MYSQL_PORT`, configurar `DB_URL` con ese mismo puerto. No se configura `spring.jpa.database-platform`: Hibernate detecta el dialecto.

- Eureka: <http://localhost:8761>.
- Configuración JSON: <http://localhost:8888/ms-dueno/default>.
- Configuración YAML: <http://localhost:8888/ms-dueno-default.yml>.
- Salud: <http://localhost:8081/actuator/health>.
- Información: <http://localhost:8081/actuator/info>.
- Swagger: <http://localhost:8081/swagger-ui/index.html>.
- OpenAPI: <http://localhost:8081/v3/api-docs>.
- API: <http://localhost:8081/api/duenos>.

Para detener la aplicación iniciada en la terminal, usar `Ctrl+C`. Para detener todos los contenedores conservando los datos:

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
      adapter/in/web/           DuenoController
        dto/                    DuenoRequest, DuenoResponse, DuenoMapper y ErrorResponse
      adapter/out/persistence/  DuenoJpaRepository y DuenoPersistenceAdapter
      config/                   GlobalExceptionHandler
      MsDuenoApplication.java
    src/main/resources/application.yml
    src/test/java/              Pruebas de casos de uso
    pom.xml
  ms-mascota/                   Modelo, puertos y capas hexagonales reservadas
  ms-veterinario/               Modelo, puertos y capas hexagonales reservadas
  ms-turno/                     Modelo, enum, puertos y capas hexagonales reservadas
  eureka-server/                Aplicación de registro en 8761
  config-server/                Servidor de configuración Git en 8888
  docs/
  compose.yml
  pom.xml
```

`DuenoController` usa el puerto `DuenoService` porque necesita el contrato de los casos de uso. Esto permite sustituir `DuenoServiceImpl` y probar el controlador con otra implementación. El controlador recibe y devuelve DTOs; `DuenoMapper` convierte los modelos mediante métodos estáticos manuales, como pide el sprint 10. Los DTOs usan Lombok y el controlador no importa `domain.model`.

`DuenoServiceImpl` recibe `DuenoRepository` por constructor y controla la unicidad de email. No importa adaptadores, JPA ni Spring Web. Las transacciones agrupan las consultas y escrituras. El adaptador de persistencia implementa ese puerto y delega en `DuenoJpaRepository`, que extiende Spring Data JPA. La composición resuelve `update(Long, Dueno)`, método que `JpaRepository` no proporciona, y evita mezclar su contrato genérico de `save` con el puerto propio.

La consigna combina un dominio independiente de frameworks con la exigencia explícita de `@Entity` y Lombok sobre `Dueno`. Se mantiene esa simplificación didáctica del paso 3: el modelo depende de Jakarta Persistence y Lombok; los puertos y excepciones son Java puro. No hay Spring ni referencias a adaptadores en el dominio. Esta dependencia de JPA impide considerarlo completamente independiente de persistencia.

### Contextos y alcance

| Módulo | Puerto | Responsabilidad | Estado en sprint 10 |
| --- | --- | --- | --- |
| `ms-dueno` | 8081 | CRUD de dueños y email único | Implementado |
| `ms-mascota` | 8082 | Mascotas y referencia al dueño | Modelo y puertos compilables |
| `ms-veterinario` | 8083 | Veterinarios y especialidades | Modelo y puertos compilables |
| `ms-turno` | 8084 | Turnos y cancelación | Modelo, enum y puertos compilables |

Cada servicio será dueño de sus datos y de su base; no habrá JOIN ni relaciones JPA entre servicios. Se comunicarán mediante APIs o eventos. Los otros tres módulos generan JARs de sus modelos y contratos. Se incluyen las entidades y operaciones nombradas por la sección 4: Mascota con duenoId y consulta por dueño; Veterinario con matrícula y consulta por especialidad; Turno con fechaHora, mascotaId, veterinarioId, motivo, estado PENDIENTE/CONFIRMADO/CANCELADO, consultas y cancelación. Aplicación y adaptadores conservan su estructura mediante archivos .gitkeep. La excepción de ausencia de Mascota se expresa por la categoría ResourceNotFoundException.

El criterio de aceptación permite que estas capas estén vacías: en esta entrega no hay servidores REST ejecutables de esos tres contextos. Los puertos 8082 a 8084 y sus bases quedan definidos en patitas-config para cuando se completen. La validación de referencias vía HTTP corresponde al sprint 13; Gateway y Circuit Breaker al sprint 11.

Los microservicios permiten desplegar y escalar por contexto y aislar fallos de proceso, pero agregan comunicación por red, configuración y observabilidad. Para el tamaño actual de la clínica el monolito sigue siendo una alternativa válida; esta separación responde al objetivo didáctico del trabajo práctico.

## Configuración centralizada y Actuator

El application.yml de ms-dueno conserva solo el nombre de la aplicación y el import de Config Server. Datasource, JPA, puerto, Eureka, información y exposición de Actuator provienen de Git. Las URL se pueden sustituir con variables de entorno para usar la misma configuración en Docker.

Config Server responde /ms-dueno/default con un objeto Environment: nombre, perfil, commit Git y propertySources. /ms-dueno-default.yml ofrece la representación YAML. Existen archivos propios para los cuatro contextos y un application.yml compartido.

El import es obligatorio: ms-dueno termina con error si Config Server no está disponible. Aunque el ejemplo del PDF usa optional, se omite ese prefijo para cumplir el flujo de arranque obligatorio. Los cambios en Git se obtienen al iniciar el microservicio; no se incorpora Spring Cloud Bus ni refresco dinámico de propiedades.

Actuator expone health, info, metrics, env, beans y mappings, como pide la consigna. Health incluye MySQL y Config Server; info informa nombre, descripción y versión. Los valores de env permanecen ocultos. Los puertos Docker se publican en 127.0.0.1 para esta demostración local. Eureka tiene self-preservation deshabilitado según la guía.

La versión 2025.0.0 del ejemplo está destinada a Boot 3.5. Se usa Spring Cloud 2025.1.4 con Boot 4.1.0, según la [matriz oficial](https://spring.io/projects/spring-cloud/), sin desactivar la verificación de compatibilidad.

Se mantiene el adaptador de persistencia por composición para evitar métodos recursivos y el manejo HTTP centralizado en config/GlobalExceptionHandler.java. Las excepciones siguen siendo reutilizables por categoría de acuerdo con AGENTS.md.

## Verificaciones

```powershell
.\mvnw.cmd validate
.\mvnw.cmd clean verify
```

Las pruebas de aplicación usan un repositorio en memoria como doble, sin levantar Spring ni MySQL. La [colección del sprint 10](docs/evidencias/sprint-10/sprint-10-infraestructura-duenos.postman_collection.json) comprueba Config Server, Eureka, Actuator y la API contra MySQL. Las [evidencias actuales](docs/evidencias/sprint-10/README.md) y las [del sprint 09](docs/evidencias/sprint-09/README.md) conservan sus ejecuciones.

Con MySQL y el servicio iniciados, ejecutar la colección sobre una base sin dueños, en el orden guardado:

```powershell
npx newman run docs/evidencias/sprint-10/sprint-10-infraestructura-duenos.postman_collection.json
```

La colección crea y elimina sus propios datos. Para ejecutar el JAR generado:

```powershell
java -jar ms-dueno/target/ms-dueno-0.0.1-SNAPSHOT.jar
```
