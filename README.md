# Patitas Felices

Sistema de gestión veterinaria de la materia Microservicios y APIs Escalables de la Universidad de Palermo. Integrante: Guillermo Gabriel Orue Marighetti.

Este repositorio continúa la Fase 2 del trabajo práctico. El [monolito VetSystem, sprints 01 a 08](https://github.com/ggomarighetti/vetSystem-Orue-Marighetti) conserva sus entregas e historial. Se toma como base el [avance de los pasos 1 a 4 del sprint 09](https://github.com/ggomarighetti/patitas-felices-base-sprint-09/tree/e5cdd7d), cuyo historial también se conserva aquí.

**Sprint 13:** Circuit Breaker y Retry con Resilience4j en los adaptadores Feign de Turnos, fallback 503, observabilidad y Rate Limiter con Redis en Gateway. Se incluyen la [consigna](docs/sprints/Sprint_13_Resilience4j.pdf) y las [evidencias actuales](docs/evidencias/sprint-13/README.md).

**Sprint 12:** API Gateway reactivo como entrada de las cuatro APIs en el puerto 8080, rutas por Eureka y LoadBalancer, CORS, logging y configuración centralizada. Se conservan la [consigna](docs/sprints/Sprint_12_APIGateway.pdf) y las [evidencias](docs/evidencias/sprint-12/README.md).

**Sprint 11:** comunicación con OpenFeign desde Turnos hacia Mascotas y Veterinarios, resolución por Eureka, validación de referencias y errores 404/503 con timeout. Se conservan la [consigna](docs/sprints/Sprint_11_OpenFeign_Comunicacion.pdf) y las [evidencias](docs/evidencias/sprint-11/README.md).

**Sprint 10:** infraestructura Eureka y Config Server, configuración centralizada en [patitas-config](https://github.com/ggomarighetti/patitas-config), Actuator y ms-dueno registrado. Se incluyen la [consigna del sprint 10](docs/sprints/Sprint_10_Eureka_ConfigServer_Actuator.pdf) y sus [evidencias](docs/evidencias/sprint-10/README.md).

**Sprint 09:** proyecto Maven multi-módulo y `ms-dueno` con API REST, MySQL, validaciones y Swagger. Se incluyen la [consigna original](docs/sprints/Sprint_09_v2.pdf), el [seguimiento](docs/sprints/README.md) y las [evidencias](docs/evidencias/sprint-09/README.md).

## Requisitos

- JDK 21, con `JAVA_HOME` apuntando al JDK. El wrapper incluido utiliza Maven 3.9.16.
- MySQL 8 con dueno_db, mascota_db, veterinario_db y turno_db, y Redis 8 para el Rate Limiter, o Docker Desktop para levantar los servicios incluidos.
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

Compose espera que Config Server esté disponible antes de iniciar Eureka. Cada microservicio espera a su propia base y a la infraestructura; Turnos también espera a Mascotas y Veterinarios. Gateway espera a los cuatro servicios y a Redis saludable. Los siete JARs se compilan para Java 21 y se ejecutan en Temurin 25 sobre Linux. El ecosistema contiene doce contenedores.

1. Config Server en 8888 clona la rama main de patitas-config.
2. Eureka Server en 8761 habilita el registro.
3. ms-mascota y ms-veterinario obtienen su configuración, conectan a sus bases y se registran en Eureka.
4. ms-turno arranca con ambos destinos disponibles. ms-dueno también se ejecuta en 8081.
5. Redis almacena los tokens del Rate Limiter dentro de la red de Compose.
6. api-gateway obtiene las rutas de Config Server, se registra en Eureka y recibe las solicitudes externas en 8080.

Eureka y LoadBalancer actualizan sus registros y cachés periódicamente. Antes de ejecutar las colecciones, esperar que las cuatro listas públicas respondan 200. Tras reiniciar un servicio puede haber una demora de propagación aunque su health ya indique UP.

En Docker, los puertos 8081 a 8084 quedan dentro de la red de Compose. Todas las llamadas de negocio se hacen a `http://localhost:8080`. Los puertos 8888 y 8761 siguen disponibles en localhost para administrar configuración y registro; los puertos MySQL permiten la administración local de las bases.

Después de recompilar un servicio, reiniciar su contenedor. `docker compose --profile app stop` conserva el volumen de MySQL.

Esta es la modalidad verificada en este equipo: el arranque directo con los JDK de Windows instalados falla en la conexión interna de `java.nio` con `Unable to establish loopback connection`. Docker permite ejecutar el mismo JAR sin modificar la configuración del sistema.

Cada contenedor MySQL crea la base de su servicio, con usuario `patitas` y contraseña de desarrollo `patitas-local`. `.env` queda excluido de Git. Para usar otra contraseña, cambiar `DB_PASSWORD` en `.env` antes de la primera ejecución. El volumen conserva los datos al detener el contenedor; las variables de inicialización se aplican al crear el volumen.

### Orden de arranque con Java local

En un entorno con Java local funcionando, iniciar las cuatro bases con `docker compose --profile app up -d --wait mysql mysql-mascota mysql-veterinario mysql-turno`. Después, desde la raíz, iniciar cada aplicación en una terminal distinta y respetar el orden:

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

Para ejecutar también los otros contextos con Java local y las bases de Compose, usar una terminal por servicio, con DB_USERNAME y DB_PASSWORD definidos igual que en el ejemplo anterior:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3307/mascota_db'
.\mvnw.cmd spring-boot:run -pl ms-mascota
```

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3308/veterinario_db'
.\mvnw.cmd spring-boot:run -pl ms-veterinario
```

Esperar que ambos figuren como UP en Eureka, y luego ejecutar:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3309/turno_db'
.\mvnw.cmd spring-boot:run -pl ms-turno
```

Cuando los cuatro servicios figuren UP en Eureka, disponer de Redis en localhost:6379 e iniciar el gateway en otra terminal. Para usar el Redis de Compose con Java local, abrir una terminal desde la raíz y ejecutar `docker compose --profile app run --rm -p 127.0.0.1:6379:6379 redis`; el proceso permanece en primer plano. El Redis utilizado por el ecosistema completo de Compose conserva su puerto interno.

```powershell
.\mvnw.cmd spring-boot:run -pl api-gateway
```

El gateway no necesita credenciales de MySQL. Con ejecución Java local, los servicios escuchan en 8081 a 8084; los clientes del sistema deben usar igualmente el puerto 8080.

Con un servidor MySQL propio, crear las cuatro bases y usar su puerto real en DB_URL. Con Compose, MySQL de Dueños está en 3306; Mascotas en 3307, Veterinarios en 3308 y Turnos en 3309.

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
| `REDIS_HOST` | `localhost` | Servidor Redis del gateway; Compose usa redis |
| `REDIS_PORT` | `6379` | Puerto Redis del gateway |

Si se cambia `MYSQL_PORT`, configurar `DB_URL` de Dueños con ese mismo puerto al ejecutar Java local. Cada microservicio recibe su propia DB_URL; dentro de Docker se usan los nombres de los cuatro servidores MySQL. No se configura `spring.jpa.database-platform`: Hibernate detecta el dialecto.

- Eureka: <http://localhost:8761>.
- Configuración JSON: <http://localhost:8888/ms-dueno/default>.
- Configuración YAML: <http://localhost:8888/ms-dueno-default.yml>.
- Configuración del gateway: <http://localhost:8888/api-gateway/default>.
- Salud del gateway: <http://localhost:8080/actuator/health>.
- Información del gateway: <http://localhost:8080/actuator/info>.
- Rutas activas: <http://localhost:8080/actuator/gateway/routes>.
- Dueños: <http://localhost:8080/api/duenos>.
- Mascotas: <http://localhost:8080/api/mascotas>.
- Veterinarios: <http://localhost:8080/api/veterinarios>.
- Turnos: <http://localhost:8080/api/turnos>.
- Circuitos de Turnos: <http://localhost:8080/ms-turno/actuator/circuitbreakers>.
- Eventos de Circuit Breaker: <http://localhost:8080/ms-turno/actuator/circuitbreakerevents>.
- Eventos de Retry: <http://localhost:8080/ms-turno/actuator/retryevents>.

Swagger y OpenAPI permanecen en cada microservicio como documentación interna. Este sprint no agrega una interfaz Swagger unificada; la colección de Postman valida el acceso público por Gateway.

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
  ms-mascota/                   API REST y persistencia independiente
  ms-veterinario/               API REST y persistencia independiente
  ms-turno/                     API REST, persistencia y validación por puertos
    src/main/java/com/patitas/turno/
      domain/port/out/          MascotaServicePort y VeterinarioServicePort
      adapter/out/feign/        Clients, adaptadores y DTOs de consumo propios
                               Circuit Breaker, Retry y predicado de errores transitorios
  eureka-server/                Aplicación de registro en 8761
  config-server/                Servidor de configuración Git en 8888
  api-gateway/                  Entrada WebFlux/Netty en 8080
    src/main/java/com/patitas/gateway/
      ApiGatewayApplication.java
      filters/LoggingFilter.java
      config/RateLimitConfiguration.java
  docs/
  compose.yml
  pom.xml
```

`DuenoController` usa el puerto `DuenoService` porque necesita el contrato de los casos de uso. Esto permite sustituir `DuenoServiceImpl` y probar el controlador con otra implementación. El controlador recibe y devuelve DTOs; `DuenoMapper` convierte los modelos mediante métodos estáticos manuales, como pide el sprint 10. Los DTOs usan Lombok y el controlador no importa `domain.model`.

`DuenoServiceImpl` recibe `DuenoRepository` por constructor y controla la unicidad de email. No importa adaptadores, JPA ni Spring Web. Las transacciones agrupan las consultas y escrituras. El adaptador de persistencia implementa ese puerto y delega en `DuenoJpaRepository`, que extiende Spring Data JPA. La composición resuelve `update(Long, Dueno)`, método que `JpaRepository` no proporciona, y evita mezclar su contrato genérico de `save` con el puerto propio.

La consigna combina un dominio independiente de frameworks con la exigencia explícita de `@Entity` y Lombok sobre `Dueno`. Se mantiene esa simplificación didáctica del paso 3: el modelo depende de Jakarta Persistence y Lombok; los puertos y excepciones son Java puro. No hay Spring ni referencias a adaptadores en el dominio. Esta dependencia de JPA impide considerarlo completamente independiente de persistencia.

### Contextos y alcance

| Módulo | Puerto interno | Responsabilidad | Estado actual |
| --- | --- | --- | --- |
| `ms-dueno` | 8081 | CRUD de dueños y email único | Implementado |
| `ms-mascota` | 8082 | Mascotas y referencia al dueño | API REST y base propia |
| `ms-veterinario` | 8083 | Veterinarios y especialidades | API REST y base propia |
| `ms-turno` | 8084 | Turnos, cancelación y validación remota | API REST, base propia y OpenFeign |
| `api-gateway` | 8080 (publicado) | Entrada HTTP, rutas, CORS y logging | WebFlux, Netty y Eureka |

Cada servicio tiene su propia base. Mascota guarda duenoId como Long y Turno guarda mascotaId y veterinarioId como Long, sin JOIN, foreign key ni relaciones JPA entre servicios. Ningún módulo importa entidades o código de dominio de otro.

Las APIs de Mascotas, Veterinarios y Turnos completan la infraestructura necesaria para el flujo del sprint 11. Las consultas y escrituras pasan por sus puertos de dominio y los adaptadores JPA implementan la persistencia. La cancelación de un turno no requiere consultar servicios remotos.

Los microservicios permiten desplegar y escalar por contexto y aislar fallos de proceso, pero agregan comunicación por red, configuración y observabilidad. Para el tamaño actual de la clínica el monolito sigue siendo una alternativa válida; esta separación responde al objetivo didáctico del trabajo práctico.

## Configuración centralizada y Actuator

El application.yml de ms-dueno conserva solo el nombre de la aplicación y el import de Config Server. Datasource, JPA, puerto, Eureka, información y exposición de Actuator provienen de Git. Las URL se pueden sustituir con variables de entorno para usar la misma configuración en Docker.

Config Server responde /ms-dueno/default con un objeto Environment: nombre, perfil, commit Git y propertySources. /ms-dueno-default.yml ofrece la representación YAML. Existen archivos propios para los cuatro contextos y un application.yml compartido.

El import es obligatorio: ms-dueno termina con error si Config Server no está disponible. Aunque el ejemplo del PDF usa optional, se omite ese prefijo para cumplir el flujo de arranque obligatorio. Los cambios en Git se obtienen al iniciar el microservicio; no se incorpora Spring Cloud Bus ni refresco dinámico de propiedades.

Actuator expone health, info, metrics, env, beans y mappings, como pide la consigna. Health incluye MySQL y Config Server; info informa nombre, descripción y versión. Los valores de env permanecen ocultos. Los puertos Docker se publican en 127.0.0.1 para esta demostración local. Eureka tiene self-preservation deshabilitado según la guía.

La versión 2025.0.0 del ejemplo está destinada a Boot 3.5. Se usa Spring Cloud 2025.1.4 con Boot 4.1.0, según la [matriz oficial](https://spring.io/projects/spring-cloud/), sin desactivar la verificación de compatibilidad.

Se mantiene el adaptador de persistencia por composición para evitar métodos recursivos y el manejo HTTP centralizado en config/GlobalExceptionHandler.java. Las excepciones siguen siendo reutilizables por categoría de acuerdo con AGENTS.md.

## Comunicación con OpenFeign — Sprint 11

TurnoServiceImpl depende de MascotaServicePort y VeterinarioServicePort. Los adaptadores MascotaFeignAdapter y VeterinarioFeignAdapter implementan existsById usando clientes declarados con los nombres exactos ms-mascota y ms-veterinario. Spring Cloud LoadBalancer obtiene los destinos de Eureka; no hay IP ni puerto fijo en los clientes.

MsTurnoApplication habilita el escaneo explícito del paquete com.patitas.turno.adapter.out.feign con EnableFeignClients. Los DTOs reducidos de consumo pertenecen a ms-turno: MascotaResponse contiene id, nombre y duenoId; VeterinarioResponse contiene id, nombre, apellido y especialidad.

Al crear un turno se consulta primero la mascota y luego el veterinario. Solo si ambos existen se asigna PENDIENTE y se guarda. PUT también valida las referencias antes de modificar el registro. Únicamente el HTTP 404 remoto se transforma en false; la aplicación lo expresa con ResourceNotFoundException y el handler central devuelve 404. Los demás errores Feign y los timeouts se traducen en RemoteServiceUnavailableException, conservando la causa, y devuelven 503 con un mensaje descriptivo. Una respuesta vacía o de otro ID también se considera inválida.

La configuración de patitas-config/ms-turno.yml usa spring.cloud.openfeign.client.config.default con connectTimeout 3000 ms y readTimeout 5000 ms. Compresión de solicitud/respuesta está habilitada, loggerLevel es BASIC y el paquete de clientes registra DEBUG. Se usa el prefijo vigente spring.cloud.openfeign, porque el prefijo feign.client del ejemplo ya no configura estos clientes. Spring Cloud utiliza Retryer.NEVER_RETRY por defecto; desde el sprint 13 los reintentos se aplican con Resilience4j sobre los adaptadores, evitando dos mecanismos de Retry superpuestos. [Referencia oficial](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html).

### APIs del flujo

Las tres APIs ofrecen GET de lista y por ID, POST (201), PUT (200) y DELETE (204). Los datos inválidos responden 400 y los recursos inexistentes, 404. Usan el mismo ErrorResponse de cinco campos que Dueños, con manejo centralizado en config/GlobalExceptionHandler.java de cada servicio.

| Servicio | Ruta base | Consultas adicionales |
| --- | --- | --- |
| Mascotas | /api/mascotas | GET /api/mascotas/dueno/{duenoId} |
| Veterinarios | /api/veterinarios | GET /api/veterinarios/especialidad/{especialidad} |
| Turnos | /api/turnos | GET /api/turnos/mascota/{mascotaId}; GET /api/turnos/veterinario/{veterinarioId} |

PATCH /api/turnos/{id}/cancelar asigna CANCELADO y devuelve 200. Los IDs de otras entidades no establecen relaciones JPA. El alta de Mascota requiere duenoId positivo; este sprint no agrega una llamada remota a Dueños.

Los ejemplos del PDF funcionan con los identificadores reales obtenidos en cada POST:

```json
{
  "nombre": "Luna",
  "especie": "Gato",
  "raza": "Siamés",
  "edad": 3,
  "duenoId": 1
}
```

La edad declarada es opcional y no negativa; se conserva la fechaNacimiento opcional del modelo anterior y no se inventa una fecha a partir de la edad. Nombre, especie y duenoId son obligatorios.

```json
{
  "nombre": "Marta",
  "apellido": "López",
  "especialidad": "Felinos",
  "email": "marta@patitas.com"
}
```

Matrícula permanece opcional para aceptar el ejemplo de la consigna. Nombre, apellido, especialidad y email válido son obligatorios.

```json
{
  "fechaHora": "2026-11-15T10:00:00",
  "motivo": "Control anual",
  "mascotaId": 1,
  "veterinarioId": 1
}
```

La fecha, el motivo y los dos IDs positivos son obligatorios. El servidor controla el ID y el estado inicial. La comprobación HTTP no crea una transacción distribuida: valida la existencia al solicitar el alta, sin bloquear ni impedir eliminaciones en los servicios remotos.

La consigna del sprint 11 prevalece sobre las vistas previas anteriores: Gateway se incorpora en el sprint 12 y la resiliencia con Circuit Breaker corresponde al sprint 13.

## API Gateway — Sprint 12

El módulo `api-gateway` usa `spring-cloud-starter-gateway-server-webflux`, Eureka Client, Spring Cloud LoadBalancer, Config Client y Actuator. El servidor es Netty reactivo; su JAR no incluye Spring MVC, servidor Tomcat, JPA ni drivers MySQL. Se conserva la versión real del padre Maven, `0.0.1-SNAPSHOT`.

El `application.yml` local contiene solamente `spring.application.name: api-gateway` y el import obligatorio de Config Server. Puerto, Eureka, rutas, CORS, Redis y Actuator provienen de [api-gateway.yml en patitas-config](https://github.com/ggomarighetti/patitas-config/blob/c369260a51fec773c2f1b7165333652ad306744b/api-gateway.yml). Los cambios de configuración se aplican al reiniciar la aplicación.

| Ruta pública en 8080 | Destino resuelto por Eureka |
| --- | --- |
| `/api/duenos/**` | `lb://ms-dueno` |
| `/api/mascotas/**` | `lb://ms-mascota` |
| `/api/veterinarios/**` | `lb://ms-veterinario` |
| `/api/turnos/**` | `lb://ms-turno` |

Los cuatro destinos usan nombres de servicio, sin direcciones fijas. Discovery Locator genera `/ms-dueno/**`, `/ms-mascota/**` y `/ms-veterinario/**`, eliminando el prefijo del servicio antes de reenviar. Por ejemplo, `/ms-dueno/api/duenos` llega a `/api/duenos`. Desde el sprint 13, `/ms-turno/**` pertenece a la ruta canónica de Turnos y usa RewritePath: comparte la cuota de Rate Limiter con `/api/turnos/**`. El locator excluye Turnos y el propio gateway.

Se implementa el paso opcional de versionado: `/v1/duenos` y `/v1/duenos/{id}` se reescriben a `/api/duenos` y `/api/duenos/{id}`. Actuator muestra actualmente **ocho rutas**: cuatro canónicas, tres automáticas de discovery y una de versionado. La entrega original del sprint 12 tenía nueve; el sprint 13 integra el alias de Turnos en la misma ruta protegida. Los identificadores automáticos pueden conservar el nombre de aplicación en mayúsculas; sus prefijos públicos son minúsculos.

`RewriteLocationResponseHeader` conserva el host público en las cabeceras `Location` de las altas, para que sus enlaces sigan pasando por 8080. CORS permite cualquier origen y cabecera en desarrollo, los métodos GET, POST, PUT, PATCH, DELETE y OPTIONS, sin credenciales. PATCH conserva la cancelación de turnos existente. También se responden las solicitudes preflight que no coinciden con una ruta.

`LoggingFilter` implementa `GlobalFilter` y `Ordered` con orden -1. También participa como `WebFilter` para registrar Actuator, preflight y rutas inexistentes. Una marca por intercambio evita registros duplicados. Cada solicitud registra método y ruta; antes de confirmar la respuesta registra el estado HTTP efectivo, milisegundos, ruta elegida y destino. Un estado implícito se registra como 200; los errores siguen propagándose al manejador correspondiente. No registra cuerpos, cabeceras ni valores de consulta.

Actuator expone `health`, `info` y `gateway`; este último tiene acceso `read-only`. `GET /actuator/gateway/routes` permite inspeccionar rutas y `POST /actuator/gateway/refresh` devuelve 404 al no exponer operaciones de escritura. Se usan los nombres actuales `spring.cloud.gateway.server.webflux` y `management.endpoint.gateway.access`, según los manuales oficiales de [WebFlux](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html), [CORS](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/cors-configuration.html) y [Actuator](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/actuator-api.html).

La verificación real de balanceo del sprint 12 levantó temporalmente dos instancias de Dueños: Eureka publicó ambas y el gateway distribuyó solicitudes entre sus dos direcciones. Después se restauró una instancia y se comprobó la recuperación. Se mantienen las cuatro bases MySQL existentes; el esquema H2 ilustrativo del PDF no implica migrar la persistencia.

## Resiliencia — Sprint 13

Los métodos `existsById` de `MascotaFeignAdapter` y `VeterinarioFeignAdapter` llevan `@CircuitBreaker` y `@Retry`, con nombres `mascotaService` y `veterinarioService`. Sus fallbacks privados reciben `Long` y `Throwable` y devuelven el mismo tipo booleano que el puerto. Lanzan la excepción de categoría `RemoteServiceUnavailableException`, con mensaje descriptivo y causa conservada; el handler existente en `config/GlobalExceptionHandler.java` responde 503. Se reutiliza esta excepción en lugar de crear otra clase equivalente llamada ServicioNoDisponibleException. No se inventan datos ni se confirma una referencia inexistente para guardar un turno.

La configuración vive en [ms-turno.yml](https://github.com/ggomarighetti/patitas-config/blob/c369260a51fec773c2f1b7165333652ad306744b/ms-turno.yml); el bootstrap local sigue conteniendo únicamente nombre e import de Config Server.

| Parámetro por servicio | Valor |
| --- | --- |
| Ventana | COUNT_BASED, últimas 10 llamadas lógicas |
| Mínimo para evaluar | 5 llamadas, contando éxitos y fallos |
| Umbral de fallos | 50% |
| Espera en OPEN | 10 segundos, transición automática a HALF_OPEN |
| Pruebas en HALF_OPEN | 3 llamadas |
| Retry | 3 intentos totales, esperas de 1 y 2 segundos |

Circuit Breaker tiene orden 1 y envuelve Retry, de orden 2. Así, el circuito cuenta una llamada lógica después de agotar los reintentos y, estando OPEN, rechaza sin volver a ejecutar Retry. Los 404 se convierten en `false`, conservan la respuesta de negocio 404 y no causan reintentos ni abren el circuito. El predicado permite reintentar errores Feign de transporte, HTTP 429 y HTTP 5xx; los otros errores de cliente y las respuestas inválidas se degradan sin reintentos.

Se usa `resilience4j-spring-boot4:2.4.0`, que integra el SPI de salud de Boot 4, y `spring-boot-starter-aspectj`, el starter AOP vigente. El BOM de Cloud usado por el proyecto aún administra Resilience4j 2.3.0; por eso el padre importa el BOM 2.4.0 antes del de Cloud y administra también el módulo Boot 4 que ese BOM omite. Los módulos hijos no fijan versiones. Las adaptaciones están fundamentadas en la [publicación oficial 2.4.0](https://github.com/resilience4j/resilience4j/releases/tag/v2.4.0), el [módulo Boot 4](https://github.com/resilience4j/resilience4j/tree/v2.4.0/resilience4j-spring-boot4) y el [manual de AOP](https://docs.spring.io/spring-boot/4.0/reference/features/aop.html).

Actuator de Turnos expone `health`, `info`, `metrics`, `circuitbreakers`, `circuitbreakerevents` y `retryevents`. La métrica `resilience4j.circuitbreaker.state` permite consultar los estados. El detalle de salud informa `state: OPEN` y estado `CIRCUIT_OPEN`; en HALF_OPEN usa `CIRCUIT_HALF_OPEN`. El agregador puede conservar UP, por lo que se comprueba el detalle de cada circuito. Readiness informa la disponibilidad del proceso y de su propia base; Compose utiliza `/actuator/health/readiness` para conservar disponible el fallback durante la caída de otra API. Recuperar un servicio también requiere que se actualicen discovery y sus cachés.

Gateway utiliza `RequestRateLimiter` con Redis: 10 tokens por segundo, capacidad 20 y costo 1. `clientIpKeyResolver` obtiene la dirección remota de la conexión, sin depender de principal, parámetros ni `X-Forwarded-For`. La cuota es compartida por los accesos canónico y con prefijo de Turnos. Los excesos devuelven 429; tras regenerar tokens vuelven a pasar. Clientes que comparten una dirección comparten también su cuota. [Referencia oficial](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html).

Redis es una dependencia de esta ejecución: Compose lo inicia, comprueba su salud y lo conecta al gateway por la red interna. Sus tokens son datos temporales y no se guardan en un volumen. La nueva consigna incorpora Rate Limiter en el sprint 13, por encima de la vista previa del sprint 12; JWT queda para una entrega posterior.

## Verificaciones

```powershell
.\mvnw.cmd validate
.\mvnw.cmd clean package
.\mvnw.cmd verify
```

Las 55 pruebas Maven incluyen las 47 existentes, seis pruebas de los proxies AOP reales y dos de la clave de Rate Limiter. Las pruebas AOP comprueban reintentos, fallback, causa original, aislamiento de circuitos, rechazo inmediato de OPEN y cierre tras tres pruebas correctas. Usan esperas de milisegundos; las pruebas del entorno real conservan los tiempos de la consigna.

La [colección actual](docs/evidencias/sprint-13/sprint-13-resiliencia.postman_collection.json) comprueba las cuatro APIs por 8080, OpenFeign, CORS, Eureka, Redis y Actuator. Las [evidencias del sprint 13](docs/evidencias/sprint-13/README.md) incluyen las caídas reales de Mascotas y Veterinarios, recuperación y una ráfaga para verificar 429 y ausencia de llegada al backend. Se conservan las entregas históricas de los [sprints 12](docs/evidencias/sprint-12/README.md), [11](docs/evidencias/sprint-11/README.md), [10](docs/evidencias/sprint-10/README.md) y [09](docs/evidencias/sprint-09/README.md); sus colecciones corresponden a sus respectivas versiones.

Con el ecosistema iniciado y las cuatro bases sin registros, ejecutar la colección en el orden guardado:

```powershell
npx newman run docs/evidencias/sprint-13/sprint-13-resiliencia.postman_collection.json --delay-request 120
```

La colección crea y elimina sus propios datos. La demora entre solicitudes mantiene esta prueba funcional dentro de la cuota; la prueba de ráfaga se ejecuta por separado. Para repetir los escenarios de caída, recuperación y límite:

```powershell
python docs/evidencias/sprint-13/verificar-resiliencia.py
```

Ese script detiene y restaura Mascotas y Veterinarios por separado, crea datos propios y los elimina al terminar. Requiere el ecosistema de Compose activo y las cuatro bases sin registros. Para ejecutar el JAR generado:

```powershell
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```
