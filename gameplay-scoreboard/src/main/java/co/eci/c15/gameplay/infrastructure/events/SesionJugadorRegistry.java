package co.eci.c15.gameplay.infrastructure.events;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Recuerda a quien pertenece cada sesion STOMP para poder detectar su desconexion. */
@Component
public class SesionJugadorRegistry {

    public record Sesion(String matchId, String userId) {}

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public void registrar(String sessionId, String matchId, String userId) {
        sesiones.put(sessionId, new Sesion(matchId, userId));
    }

    public Optional<Sesion> retirar(String sessionId) {
        return Optional.ofNullable(sesiones.remove(sessionId));
    }
}
