# Patitas Felices

Proyecto de microservicios del dominio veterinario para la materia Microservicios y APIs Escalables.
Continúa el aprendizaje iniciado con el monolito VetSystem en una estructura independiente.

**Estado: Sprint 9, pasos 1 a 4 implementados.** `ms-dueno` contiene el dominio y la aplicación,
incluida la validación de email duplicado al guardar. Compila y se prueba sin MySQL ni un servidor web.
La API, la persistencia real y el arranque del servicio corresponden a los pasos 5 a 7.

La consigna original y el seguimiento están en [docs/sprints](docs/sprints/README.md).

## Requisitos y verificación

- JDK 21 o posterior compatible con Spring Boot 4.1.0; `JAVA_HOME` debe apuntar al JDK.
- Maven 3.9.16 mediante el wrapper incluido. No hace falta instalar Maven por separado.
- Acceso a Maven Central en la primera compilación para descargar dependencias.

Desde la raíz, en PowerShell:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd validate
.\mvnw.cmd clean verify
```

Para compilar y probar solamente `ms-dueno`:

```powershell
.\mvnw.cmd clean package -pl ms-dueno
```

En Linux/macOS se usa `sh ./mvnw` en lugar de `.\mvnw.cmd`. Si Maven ya está instalado,
también se pueden ejecutar los mismos objetivos con `mvn`.

Las pruebas unitarias ejercitan el servicio a través de `DuenoService` usando un doble en memoria
del puerto `DuenoRepository`, exclusivamente en `src/test`. Comprueban consultas, alta, rechazo
de emails duplicados sin escritura, actualización, eliminación y propagación de fallos del repositorio.
No levantan Spring ni requieren una base de datos.

## Módulos y límites del dominio

| Módulo | Puerto previsto | Responsabilidad | Estado |
| --- | --- | --- | --- |
| `ms-dueno` | 8081 | Dueños y validaciones de negocio | Dominio y aplicación |
| `ms-mascota` | 8082 | Mascotas y referencia al dueño | POM reservado |
| `ms-veterinario` | 8083 | Veterinarios y especialidades | POM reservado |
| `ms-turno` | 8084 | Turnos y reglas de solapamiento | POM reservado |

El POM padre declara los cuatro módulos y centraliza versiones, dependencias y procesadores
de Lombok/MapStruct. Los módulos reservados usan `packaging=pom` hasta incorporar código;
así el reactor completo es válido sin generar servicios vacíos. No hay dependencias Java entre servicios.
Cada contexto será dueño de su base de datos; la comunicación entre contextos se realizará mediante
APIs o eventos, sin relaciones JPA ni JOIN entre bases de servicios distintos.

## Arquitectura

```text
patitas-felices/
  docs/sprints/                  Consignas originales y seguimiento
  ms-dueno/
    src/main/java/com/patitas/dueno/
      domain/
        model/                  Dueno
        exception/              DuplicatedEmailException
        port/in/                DuenoService
        port/out/               DuenoRepository
      application/              DuenoServiceImpl
      adapter/in/web/dto/        Reservado para el paso 6
      adapter/out/persistence/   Reservado para el paso 5
    src/main/resources/         application.yml
    src/test/java/              Pruebas unitarias de aplicación
    pom.xml
  ms-mascota/pom.xml
  ms-veterinario/pom.xml
  ms-turno/pom.xml
  pom.xml
```

Las dependencias apuntan hacia el dominio. `DuenoServiceImpl` implementa el puerto de entrada
y recibe el puerto de salida por constructor. No conoce adaptadores, Spring Web ni JPA.
Su única anotación de Spring es `@Service`, exigida por la consigna.

El futuro controlador dependerá de `DuenoService`, no de `DuenoServiceImpl`: el contrato de los casos
de uso permite cambiar su implementación y probar el controlador sin acoplarlo al servicio concreto.

### Decisiones para los pasos 1 a 4

- El PDF describe un dominio sin frameworks, pero su paso 3 exige JPA y Lombok sobre `Dueno`.
  Se cumple ese paso con `jakarta.persistence` y las anotaciones de Lombok indicadas.
  Es una simplificación didáctica: el modelo no es completamente independiente de persistencia.
  Los puertos y la excepción son Java puro, sin Spring ni adaptadores.
- `findById` devuelve `Optional<Dueno>` para expresar ausencia sin devolver `null`.
  Ambos puertos comparten las cinco operaciones CRUD requeridas.
- Se agrega `existsByEmail(String)` al puerto de salida ahora porque la validación del paso 4
  necesita ese contrato; su implementación de persistencia queda para el paso 5.
- `save` consulta el email antes de escribir y lanza `DuplicatedEmailException` si ya existe.
  Las demás operaciones delegan al puerto de salida como indica el paso 4.
- La comparación de emails la resolverá el adaptador de persistencia. Al implementarlo habrá
  que definir su tratamiento de mayúsculas/minúsculas y asegurar unicidad en la base de datos
  para cubrir altas concurrentes. La comprobación previa del servicio por sí sola no garantiza esto.
- El JAR actual no es ejecutable: la clase principal y el empaquetado de Spring Boot se agregan
  en el paso 7. Todavía no corresponde ejecutar `spring-boot:run` ni probar Swagger.

## Configuración de MySQL para el siguiente paso

`application.yml` deja preparados `server.port=8081`, `spring.application.name=ms-dueno`,
`ddl-auto=update` y la conexión a `dueno_db`. Hibernate detectará el dialecto; no se configura
`spring.jpa.database-platform`.

| Variable de entorno | Valor por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/dueno_db` | Conexión del servicio |
| `DB_USERNAME` | `root` | Usuario local, reemplazable |
| `DB_PASSWORD` | Vacío | Contraseña del usuario MySQL |

Cuando se complete el arranque del servicio, crear previamente `dueno_db` en MySQL y definir
estas variables en el entorno o en la configuración de ejecución del IDE. No se guardan credenciales
en el repositorio. Los tests y la compilación actuales no crean ni modifican bases de datos.

## Referencias técnicas

- [Publicación oficial de Spring Boot 4.1.0](https://spring.io/blog/2026/06/10/spring-boot-4/).
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/).
- [Compatibilidad entre Lombok y MapStruct](https://mapstruct.org/faq/#Can-I-use-MapStruct-together-with-Project-Lombok).
