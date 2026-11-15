# Qabit Test – Backend Room Booking

Microservicio REST para gestionar salas de reuniones y sus reservas, hecho con Java 25 y Spring Boot 3.5.9. Permite crear salas, reservarlas por franjas horarias, consultar y filtrar reservas, actualizarlas y cancelarlas, sin permitir solapamientos en una misma sala.

Lo desarrollé como prueba técnica para una posición Backend Senior. La idea no era solo que los endpoints respondieran, sino dejar un servicio con validaciones, errores consistentes, pruebas que demuestren el comportamiento y una base de datos versionada con Flyway. Le dediqué aproximadamente 8 horas, repartidas entre análisis, implementación, pruebas y documentación.

Tecnologías utilizadas: Spring Web, Spring Data JPA, Spring Security con JWT, Flyway, H2 y PostgreSQL, springdoc para OpenAPI, y Lombok y MapStruct para no repetir código. Las pruebas están hechas con JUnit 5, Mockito y MockMvc.

## Cómo ejecutarlo

Hace falta JDK 25 y Maven 3.9 o superior. Docker solo es necesario si quieres probar contra PostgreSQL.

Puedes comprobar qué JDK está usando Maven con `mvn -version`. Si no muestra Java 25, apunta `JAVA_HOME` al JDK 25 antes de compilar:

```powershell
$env:JAVA_HOME='C:\ruta\al\jdk-25'
mvn -version
```

### La opción rápida: H2 en memoria

El perfil `dev` viene activo por defecto y no necesita nada externo. Al arrancar, Flyway aplica la migración sobre una base H2 en memoria.

```bash
mvn spring-boot:run
```

Con eso queda disponible:

- API en <http://localhost:8081>
- Swagger UI en <http://localhost:8081/swagger-ui.html>
- Documento OpenAPI en <http://localhost:8081/v3/api-docs>
- Consola H2 en <http://localhost:8081/h2-console>, con JDBC URL `jdbc:h2:mem:roombooking`, usuario `sa` y contraseña vacía

Como la base es en memoria, los datos se pierden al reiniciar. Para desarrollo me venía bien, porque cada arranque parte de un esquema limpio.

### La opción con PostgreSQL

Incluí un `docker-compose.yml` que levanta `postgres:16-alpine` con la base `roombooking`, usuario `admin` y contraseña `admin123`.

```bash
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

El contenedor tiene healthcheck, así que conviene esperar a que `docker compose ps` lo muestre como `healthy` antes de arrancar la aplicación. También se puede usar `docker compose up -d --wait`.

Para parar sin perder datos:

```bash
docker compose down
```

Y si quieres empezar de cero y que Flyway vuelva a aplicar la migración:

```bash
docker compose down -v
```

### Perfil `prod` y variables

El perfil `prod` no trae valores por defecto para los secretos: la aplicación falla al arrancar si no se los das. Es intencionado.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/roombooking
export DB_USERNAME=rooms
export DB_PASSWORD=rooms
export JWT_SECRET="una-clave-larga-y-aleatoria"
export APP_ADMIN_USER=admin
export APP_ADMIN_PASSWORD=una-contraseña-segura
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
```

En `prod` el puerto no está fijado, así que usa el 8080 por defecto. Para cambiarlo en cualquier perfil: `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"`.

| Variable | Para qué sirve | Valor por defecto |
| --- | --- | --- |
| `BOOKING_ZONE` | Zona horaria del filtro `date` | `UTC` |
| `JWT_SECRET` | Clave HMAC de los tokens | Solo en `dev`/`postgres` |
| `APP_ADMIN_USER` | Usuario que puede autenticarse | `admin` en `dev`/`postgres` |
| `APP_ADMIN_PASSWORD` | Contraseña de ese usuario | `admin123` en `dev`/`postgres` |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/roombooking` |
| `DB_USERNAME` | Usuario de base de datos | `admin` en `postgres`, `rooms` en `prod` |
| `DB_PASSWORD` | Contraseña de base de datos | `admin123` en `postgres`, `rooms` en `prod` |

### Empaquetado

```bash
mvn clean package
java -jar target/qabit_test_back_room_booking-1.0.0.jar
```

## Cómo está organizado el código

Aplicé la arquitectura hexagonal porque la prueba tenía reglas de negocio muy específicas (solapes, cancelaciones, orden de errores, validación de intervalos) y porque quería poder probarlas sin levantar Spring ni una base de datos. Separar dominio, aplicación, infraestructura y web me dio esa libertad: así el dominio solo conoce interfaces puras (sin Spring ni JPA), los servicios implementan la lógica de negocio y las capas de infraestructura (JPA, repositorios, mappers) viven en `infrastructure`, y los controladores son capas finas que solo traducen HTTP a llamadas de casos de uso.

La regla que intenté respetar en todo momento es que el dominio no sepa nada de Spring ni de JPA. Por eso los repositorios del dominio son interfaces propias y los adaptadores de persistencia viven en `infrastructure`. Los controladores no llaman a repositorios: hablan con puertos de entrada.

```
src/main/java/jetpackcodesolution/
├── domain/          modelo, puertos de repositorio y excepciones de negocio
├── application/     casos de uso, servicios, puertos de entrada/salida y DTOs
├── infrastructure/  JPA, especificaciones, mappers y seguridad (JWT)
├── web/             controladores, requests, responses y manejo de errores
└── config/          seguridad, OpenAPI y beans
```

En `domain` están `Room`, `Booking` y las excepciones que representan reglas incumplidas. En `application` están los servicios que implementan cada caso de uso y las interfaces que necesita el dominio para persistir. `infrastructure` implementa esas interfaces con Spring Data JPA. `web` traduce HTTP a llamadas de casos de uso y de vuelta.

## Decisiones que tomé

### Intervalos semiabiertos

Una reserva ocupa `[inicio, fin)`: el inicio entra y el fin no. Con intervalos cerrados, una reserva de 10:00 a 11:00 y otra de 11:00 a 12:00 se considerarían solapadas, y eso no tiene sentido en una sala de reuniones.

La condición que uso para detectar solapes es:

```
existingStart < requestedEnd && existingEnd > requestedStart
```

Además valido que `fin > inicio` y que la reserva empiece en el futuro. Los dos errores devuelven 400 con mensajes distintos, porque para el cliente no es lo mismo equivocarse en el orden de las fechas que intentar reservar en el pasado.

### Concurrencia: bloqueo pesimista sobre la sala

Este fue el punto que más pensé. Lo fácil es consultar si hay solapes y, si no hay, insertar. El problema es que dos peticiones simultáneas pueden consultar a la vez, no ver nada y crear dos reservas incompatibles. La validación en aplicación sola no lo evita.

Opté por bloquear el registro de la sala dentro de la transacción, con un `SELECT ... FOR UPDATE` (en JPA, `PESSIMISTIC_WRITE`). El flujo queda así: se abre la transacción, se bloquea la sala, se buscan solapes, se inserta si no hay conflicto y se confirma. Mientras una petición tiene el bloqueo, la otra espera.

Tomé la misma precaución al actualizar (bloqueo la reserva y la sala destino) y al cancelar (bloqueo la reserva). Es una solución que serializa las escrituras por sala, y eso reduce el paralelismo, pero para este dominio es asumible y mucho más simple de razonar que reintentos con bloqueo optimista. Preferí consistencia y claridad.

### Cancelación lógica e idempotente

Cancelar no borra la fila: escribe `cancelled_at`. Así se conserva el historial y se puede auditar qué pasó. La operación es idempotente: si la reserva ya estaba cancelada, vuelve a responder 204 y no toca el instante original. Esto lo probé explícitamente porque es fácil escribir una versión que actualice la fecha en cada llamada.

Una reserva cancelada libera su hueco, pero no se puede modificar: un `PUT` sobre ella devuelve 409. Tampoco dejo borrar una sala que tenga reservas, aunque estén canceladas, porque forman parte del histórico.

### Errores y orden de validación

Todos los errores usan `ProblemDetail` (RFC 7807), con `title`, `status`, `detail` e `instance`. Los de validación añaden una lista `errors` con el campo y el mensaje, para que el cliente sepa qué corregir sin tener que adivinar.

El orden que fijé es 400 → 404 → 409: primero los datos de entrada, después la existencia del recurso y al final los conflictos de negocio. Mantenerlo igual en todos los endpoints me obligó a ordenar las comprobaciones dentro de los servicios, y también a decidir qué pasa cuando hay varios errores a la vez. Por ejemplo, en la actualización de una reserva: si el intervalo es inválido, responde 400 aunque la reserva no exista; si la reserva no existe, 404 aunque la sala tampoco; y si está cancelada, 409 antes de comprobar solapes.

### Consultas, filtros y paginación

El listado de reservas acepta `roomId`, `date`, `includeCancelled`, `page` y `size`. El orden es fijo por `startTime` y luego por `id`, para que la paginación sea determinista; ignoro el `sort` que mande el cliente porque no forma parte del contrato.

El filtro `date` se interpreta en la zona configurada en `BOOKING_ZONE` (UTC por defecto) y devuelve las reservas que solapan ese día, no solo las que empiezan dentro. Eso hace que una reserva que empieza a las 23:00 del día anterior aparezca en el día consultado, que es lo coherente si estás mirando la ocupación.

Devuelvo un `PageResponse` propio (`content`, `page`, `size`, `totalElements`, `totalPages`) en lugar de serializar el `Page` de Spring. Así el contrato de la API no queda atado a una clase del framework.

Valido `page` y `size` a mano: página negativa y tamaño fuera de 1..100 devuelven 400. El límite de 100 evita que una petición se lleve media base de datos.

### Seguridad

Las lecturas son públicas y las escrituras requieren JWT, tal como pedía el enunciado. Hay un único usuario administrador configurable por variables de entorno, y el token se firma con HMAC y caduca en una hora.

Es la parte más sencilla del proyecto y también la que más ampliaría: hoy cualquier usuario autenticado puede cancelar cualquier reserva, porque no hay propietario. Lo dejé así por alcance, no porque no viera el problema.

### Base de datos y migraciones

Uso Flyway desde el primer día y `ddl-auto: validate`. Si el esquema y las entidades se separan, la aplicación no arranca; prefiero que falle al inicio a que lo haga en producción con una columna que no existe.

Las tablas usan `timestamptz` para las fechas, identidades `BIGINT` y restricciones a nivel de base de datos (`capacity > 0`, `end_time > start_time`, nombre único, clave foránea). Las validaciones de aplicación están, pero la base también defiende sus invariantes.

## Los endpoints

Todas las rutas cuelgan de `/api`. Las columnas de códigos indican el éxito y los errores posibles.

| Método | Ruta | Auth | Éxito | Errores |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | No | 200 | 400, 401 |
| GET | `/api/rooms` | No | 200 | — |
| GET | `/api/rooms/{id}` | No | 200 | 400, 404 |
| POST | `/api/rooms` | Bearer | 201 | 400, 401, 409 |
| PUT | `/api/rooms/{id}` | Bearer | 200 | 400, 401, 404, 409 |
| DELETE | `/api/rooms/{id}` | Bearer | 204 | 400, 401, 404, 409 |
| POST | `/api/bookings` | Bearer | 201 | 400, 401, 404, 409 |
| GET | `/api/bookings` | No | 200 | 400 |
| GET | `/api/bookings/{id}` | No | 200 | 400, 404 |
| PUT | `/api/bookings/{id}` | Bearer | 200 | 400, 401, 404, 409 |
| DELETE | `/api/bookings/{id}` | Bearer | 204 | 400, 401, 404 |

Un ejemplo completo, empezando por el login:

```bash
BASE=http://localhost:8081

TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

curl -X POST $BASE/api/rooms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Sala Azul","capacity":8}'

curl -X POST $BASE/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roomId":1,"responsible":"Ana Pérez","startTime":"2030-01-15T10:00:00Z","endTime":"2030-01-15T11:00:00Z"}'

curl "$BASE/api/bookings?roomId=1&date=2030-01-15"
```

Un solapamiento devuelve algo así:

```json
{
  "type": "about:blank",
  "title": "Conflicto de reserva",
  "status": 409,
  "detail": "Ya existe una reserva que se solapa en esa sala.",
  "instance": "/api/bookings"
}
```

En PowerShell, `curl` es un alias de `Invoke-WebRequest`; hay que usar `curl.exe` o adaptar las llamadas a `Invoke-RestMethod`.

## Pruebas

La suite tiene 124 pruebas y pasa entera con JDK 25.

```bash
mvn test
```

Hay dos niveles. Las unitarias prueban los servicios con Mockito: reglas de solape, orden de validaciones, cancelación idempotente, conflictos de nombre. Las de integración usan MockMvc y comprueban el contrato HTTP de verdad: códigos, cuerpos, cabeceras, seguridad, paginación y el JSON de OpenAPI.

También incluí pruebas de concurrencia con hilos reales sobre H2. No son infalibles, pero sirven para lo que quería: si quito el bloqueo pesimista, se ponen en rojo. Eso me confirmó que la prueba detecta el problema y no es decorativa.

Aparte de la suite, verifiqué la aplicación a mano con `curl` y PowerShell. Primero contra H2 y después contra PostgreSQL real: autenticación, CRUD de salas, creación y actualización de reservas, solapes, filtros, paginación, cancelación doble, borrado de salas con y sin historial, y los códigos 400, 401, 404 y 409. También revisé el esquema en PostgreSQL (`\d rooms`, `\d bookings`) y que Flyway hubiera registrado la migración.

## Qué haría con más tiempo

Lo primero sería añadir propiedad a las reservas: guardar el usuario que la crea y permitir que solo él (o un administrador) la cancele. Hoy el token identifica a un administrador global, y eso es una simplificación grande.

Después cambiaría las pruebas de integración de H2 a Testcontainers con PostgreSQL. H2 en modo PostgreSQL se parece bastante, pero no es lo mismo, y para un servicio que va a producción prefiero probar contra el motor real. Eso implica que los tests necesiten Docker, y habría que decidir si eso es aceptable en CI.

En la API añadiría un endpoint de disponibilidad por sala y día, para que el cliente no tenga que calcular los huecos a partir del listado. También revisaría la paginación por offset si el volumen crece; con muchas reservas, keyset pagination sería más eficiente.

En infraestructura faltan un `Dockerfile` para la aplicación, un compose que levante API y base de datos juntas, y un pipeline de GitHub Actions que ejecute las pruebas en cada push. Sumaría Actuator, métricas y logs estructurados.

Y en seguridad, refresh tokens, rotación de secretos y límites de peticiones. Nada de eso era necesario para la prueba, pero es lo que separa un ejercicio de un servicio en producción.

## Cómo usé la IA

La usé como herramienta de trabajo, no como piloto automático. El flujo fue siempre el mismo: yo decidía qué tocaba, la IA proponía una implementación o unos casos de prueba, yo la revisaba, ejecutaba todo y solo entonces hacía el commit. Ningún commit lo hizo la IA, y ninguna decisión de alcance se tomó sin que yo la validara.

Antes de escribir código hice el análisis de requisitos y el plan por funcionalidades. Ahí la IA me ayudó a enumerar riesgos (solapes, concurrencia, orden de errores) y a contrastar la estructura hexagonal. Después fui funcionalidad a funcionalidad: salas, reservas, consultas, actualización, cancelación.

### Qué le pedí

Le pedí cosas concretas y acotadas: que propusiera la estructura de paquetes y los puertos, que generara esqueletos de entidades y DTOs, que escribiera la migración inicial, que plantease escenarios límite para los tests (reservas consecutivas, solape en el borde, cancelación doble, peticiones concurrentes) y que revisara la estrategia de bloqueo. También le pedí que redactara documentación a partir de lo ya implementado, no al revés.

En las partes delicadas le pedí que explicara el razonamiento antes de darme código. Por ejemplo, con la concurrencia: primero discutimos por qué una comprobación en aplicación no basta, y solo después implementamos el bloqueo sobre la sala.

### Qué acepté sin cambios

Acepté bastante andamiaje: entidades JPA, DTOs de entrada y salida, mappers de MapStruct, la migración de Flyway, el manejador centralizado de errores con `ProblemDetail` y el esqueleto de muchos tests. También acepté la idea de usar intervalos semiabiertos, que luego verifiqué con casos de borde.

Ese código lo leí antes de incorporarlo. No porque desconfiara por sistema, sino porque es mi nombre el que va en el repositorio.

### Qué corregí

Esta es la parte que más me interesa contar, porque es donde se ve si la IA fue una herramienta o una excusa. Algunos ejemplos reales:

- Swagger UI devolvía 401 en las operaciones protegidas aunque hubiera token. El esquema `bearerAuth` estaba declarado, pero ninguna operación lo referenciaba, así que la interfaz no enviaba la cabecera. Añadí `@SecurityRequirement` en cada operación protegida y un test que comprueba que `/v3/api-docs` lo incluye.
- La primera versión usaba `Specification.where()`, que ya no encaja con la API actual de Spring Data. La sustituí por una alternativa compatible y volví a ejecutar las pruebas de filtros.
- La consulta de solapes de la actualización tenía que excluir el `id` de la propia reserva; si no, cualquier actualización choca consigo misma. Lo dejé cubierto con un test para que no vuelva a pasar.
- Los tests comprobaban `cancelledAt` con `doesNotExist()`, pero Jackson serializa el campo como `null`. Fallaban aunque la respuesta fuera correcta. Los cambié a `nullValue()`. Me pasó dos veces, la segunda por copiar el patrón de un test antiguo.
- Un test de la fase de creación esperaba un 404 en `GET /api/bookings/{id}` para dejar constancia de que el endpoint aún no existía. Era un recordatorio, no una prueba útil, y lo eliminé antes de commitear.
- Un test de filtros por fecha esperaba mal el orden. La reserva que empieza a las 23:00 del día anterior va antes que la de las 00:00 del día consultado, y yo estaba mezclando "día en el que aparece" con "inicio dentro del día". El código estaba bien; el test no.
- Un test de concurrencia usaba `andExpect(status -> ...)`, que no compila porque `andExpect` espera un `ResultMatcher`, no un `Consumer` de AssertJ. Lo cambié por `status().isNoContent()`.
- Un test unitario se contradecía: verificaba que no se tocaba el repositorio al cancelar una reserva inexistente, cuando el propio servicio lo consulta primero. Lo borré; el caso ya estaba cubierto por el test de 404.
- El mensaje de error para reservas en el pasado era ambiguo. Lo dejé en "La reserva debe comenzar posterior al momento actual", que dice exactamente qué falla.

También corregí cosas de entorno: el JDK por defecto es 21, así que las ejecuciones fijan `JAVA_HOME` a 25; y los scripts manuales de PowerShell tipaban un parámetro como `[long]`, lo que impedía probar `PUT /api/bookings/abc` porque PowerShell fallaba antes de enviar la petición.

### Cómo mantuve el control

Trabajé por funcionalidades, con commits separados de implementación y de pruebas. Antes de cada commit ejecutaba la suite completa y hacía pruebas HTTP manuales. Si algo no me cuadraba, lo investigaba aunque los tests estuvieran en verde: los tests también pueden estar mal escritos, como demuestran varios de los ejemplos anteriores.

La IA fue rápida para el andamiaje y para proponer escenarios que no había considerado. A cambio, metió la pata en detalles de API, en aserciones y en supuestos sobre el framework. La red que puse fue siempre la misma: leer el diff, compilar, ejecutar y probar a mano. Sin eso, no habría aceptado el código.

## Limitaciones conocidas

- Hay un único usuario administrador; no existe el concepto de propietario de una reserva.
- El perfil `dev` usa H2 en memoria, así que los datos no sobreviven a un reinicio.
- Las pruebas de integración corren sobre H2, no sobre PostgreSQL. La verificación contra PostgreSQL fue manual.
- El bloqueo pesimista serializa las escrituras de una misma sala. Es suficiente para este alcance, pero limita el rendimiento si hay mucha demanda sobre la misma sala.
- No hay refresh tokens ni revocación de tokens.
- El perfil `prod` exige configurar los secretos por variables de entorno.

## Cierre

El resultado cubre los once endpoints, las reglas de solape y concurrencia, la cancelación lógica, los filtros con paginación, la seguridad con JWT y las migraciones con Flyway, con 124 pruebas en verde y una verificación completa contra PostgreSQL.

Si tuviera que resumir lo que aprendí con la prueba, diría que lo difícil no fue escribir los endpoints, sino decidir qué significa "no se solapan" y asegurarme de que esa decisión se cumpliera también cuando llegan dos peticiones a la vez. Ahí es donde la IA ayudó a pensar y donde yo tuve que tomar la decisión final.
