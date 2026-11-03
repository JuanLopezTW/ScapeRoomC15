package co.eci.c15.salas.infrastructure.events;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda que jugador esta en el lobby de que sala con cada sesion STOMP, para detectar cuando se va. Las
 * sesiones de esta instancia quedan aqui (la desconexion la ve la misma instancia que tiene el socket); si
 * el jugador sigue en la sala por otra conexion, quiza en otra instancia, lo dice {@link PresenciaSala}.
 */
@Component
public class SesionSalaRegistry {

    public record Sesion(String salaId, String userId) {}

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final PresenciaSala presencia;

    public SesionSalaRegistry(PresenciaSala presencia) {
        this.presencia = presencia;
    }

    public void registrar(String sessionId, String salaId, String userId) {
        Sesion anterior = sesiones.put(sessionId, new Sesion(salaId, userId));
        if (anterior != null && !anterior.equals(new Sesion(salaId, userId))) {
            presencia.salir(sessionId, anterior.salaId(), anterior.userId());
        }
        presencia.entrar(sessionId, salaId, userId);
    }

    public Optional<Sesion> retirar(String sessionId) {
        Optional<Sesion> sesion = Optional.ofNullable(sesiones.remove(sessionId));
        sesion.ifPresent(s -> presencia.salir(sessionId, s.salaId(), s.userId()));
        return sesion;
    }

    /** Si el jugador sigue con otra pestaña abierta en esa sala. */
    public boolean tieneOtraSesion(String salaId, String userId) {
        return presencia.conectado(salaId, userId);
    }
}
