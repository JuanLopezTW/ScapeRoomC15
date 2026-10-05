package co.eci.c15.gameplay.application;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el lock optimista de acertijos por equipo.
 * Clave: acertijoId  →  userId que lo tiene bloqueado.
 * El lock es en memoria; si el servidor reinicia se libera (aceptable para MVP).
 */
@Service
public class AcertijoLockService {

    private final Map<String, String> locks = new ConcurrentHashMap<>();

    /** Intenta bloquear el acertijo para userId. Devuelve true si lo obtuvo. */
    public boolean bloquear(String acertijoId, String userId) {
        String anterior = locks.putIfAbsent(acertijoId, userId);
        return anterior == null || anterior.equals(userId);
    }

    /** Libera el bloqueo solo si userId es quien lo tiene. */
    public void liberar(String acertijoId, String userId) {
        locks.remove(acertijoId, userId);
    }

    /** Libera el bloqueo incondicionalmente (desconexión). */
    public void liberarForzado(String acertijoId) {
        locks.remove(acertijoId);
    }

    public Optional<String> propietario(String acertijoId) {
        return Optional.ofNullable(locks.get(acertijoId));
    }

    public boolean estaBloqueadoPorOtro(String acertijoId, String userId) {
        String propietario = locks.get(acertijoId);
        return propietario != null && !propietario.equals(userId);
    }
}
