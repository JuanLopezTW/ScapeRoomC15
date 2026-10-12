package co.eci.c15.salas.infrastructure.events;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Recuerda que jugador esta en el lobby de que sala con cada sesion STOMP, para detectar cuando se va. */
@Component
public class SesionSalaRegistry {

    public record Sesion(String salaId, String userId) {}

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public void registrar(String sessionId, String salaId, String userId) {
        sesiones.put(sessionId, new Sesion(salaId, userId));
    }

    public Optional<Sesion> retirar(String sessionId) {
        return Optional.ofNullable(sesiones.remove(sessionId));
    }

    /** Si el jugador sigue con otra pestaña abierta en esa sala. */
    public boolean tieneOtraSesion(String salaId, String userId) {
        return sesiones.containsValue(new Sesion(salaId, userId));
    }
}
