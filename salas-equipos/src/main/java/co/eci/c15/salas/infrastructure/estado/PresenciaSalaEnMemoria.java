package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.salas.infrastructure.events.PresenciaSala;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Con una sola instancia basta mirar las conexiones de esta. */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class PresenciaSalaEnMemoria implements PresenciaSala {

    private record Quien(String salaId, String userId) {}

    private final Map<String, Quien> sesiones = new ConcurrentHashMap<>();

    @Override
    public void entrar(String sessionId, String salaId, String userId) {
        sesiones.put(sessionId, new Quien(salaId, userId));
    }

    @Override
    public void salir(String sessionId, String salaId, String userId) {
        sesiones.remove(sessionId);
    }

    @Override
    public boolean conectado(String salaId, String userId) {
        return sesiones.containsValue(new Quien(salaId, userId));
    }

    @Override
    public boolean hayAlguien(String salaId) {
        return sesiones.values().stream().anyMatch(q -> q.salaId().equals(salaId));
    }
}
