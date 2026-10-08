package co.eci.c15.gameplay.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Estado de un acertijo para un equipo dentro de una partida: si está resuelto y quién
 * lo tiene abierto. Solo un jugador del equipo puede tenerlo abierto a la vez.
 * Los cambios son atómicos (métodos sincronizados).
 */
public final class AcertijoEnPartida {

    public enum Estado { PENDIENTE, RESUELTO }

    private final String matchId;
    private final String equipoId;
    private final String componenteMapaId;
    private Estado estado = Estado.PENDIENTE;
    private String poseedor;
    private Instant bloqueadoEn;

    public AcertijoEnPartida(String matchId, String equipoId, String componenteMapaId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
        this.componenteMapaId = Objects.requireNonNull(componenteMapaId);
    }

    /**
     * Bloquea el acertijo para el jugador. Se puede tomar si está libre, si ya era suyo, si el
     * bloqueo venció (ttl) o si quien lo tenía ya no sigue en la partida (se desconectó).
     *
     * @return el jugador al que se le quitó el bloqueo, si había otro
     * @throws AcertijoBloqueadoException si otro jugador lo tiene abierto
     */
    public synchronized Optional<String> bloquear(String userId, Instant ahora, Duration ttl,
                                                  Predicate<String> sigueEnPartida) {
        Objects.requireNonNull(userId);
        if (poseedor != null && !poseedor.equals(userId)
                && !vencido(ahora, ttl) && sigueEnPartida.test(poseedor)) {
            throw new AcertijoBloqueadoException(componenteMapaId);
        }
        String anterior = poseedor != null && !poseedor.equals(userId) ? poseedor : null;
        poseedor = userId;
        bloqueadoEn = ahora;
        return Optional.ofNullable(anterior);
    }

    /** Libera el bloqueo solo si lo tiene ese jugador. */
    public synchronized void liberar(String userId) {
        if (userId != null && userId.equals(poseedor)) {
            poseedor = null;
            bloqueadoEn = null;
        }
    }

    public synchronized void marcarResuelto() {
        estado = Estado.RESUELTO;
        poseedor = null;
        bloqueadoEn = null;
    }

    private boolean vencido(Instant ahora, Duration ttl) {
        return bloqueadoEn != null && !ahora.isBefore(bloqueadoEn.plus(ttl));
    }

    public synchronized boolean isResuelto() { return estado == Estado.RESUELTO; }
    public synchronized Estado getEstado() { return estado; }
    public synchronized Optional<String> getPoseedor() { return Optional.ofNullable(poseedor); }
    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
    public String getComponenteMapaId() { return componenteMapaId; }
}
