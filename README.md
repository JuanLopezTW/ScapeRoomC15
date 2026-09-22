# EscapeRoomC15

Monolito modular para el proyecto de ARSW (curso de arquitectura de software) del equipo C15-PortalSuba:
un escape room multijugador en tiempo real.

## Modulos

- `common` — eventos de dominio, DTOs y excepciones compartidas.
- `auth-perfil` — autenticacion basica y perfil.
- `salas-equipos` — catalogo de salas y formacion de equipos.
- `gameplay-scoreboard` — logica de partida en tiempo real y puntaje.
- `chat` — mensajeria global y por sala.
- `estadisticas` — reportes y metricas.
- `app` — arranque unico de Spring Boot que ensambla todos los modulos anteriores.

## Por que esta estructura

Cada modulo separa `domain` (reglas de negocio puras, sin Spring),
`application` (casos de uso) e `infrastructure` (controllers, JPA, eventos).
La comunicacion entre modulos se hace por eventos (`common/events`), publicados
hoy en memoria via `ApplicationEventPublisher`. El dia que se decida separar
un modulo en su propio microservicio, solo cambia la capa `infrastructure/events`
(pasa de memoria a un broker como RabbitMQ/Kafka); `domain` y `application`
no se tocan.

## Correr el proyecto

```bash
mvn clean install
cd app
mvn spring-boot:run
```
