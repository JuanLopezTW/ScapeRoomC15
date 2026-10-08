package co.eci.c15.common.events;

import java.util.List;
import java.util.Map;

/**
 * La partida arranco. Cada equipo (equipoId) trae la lista de sus jugadores (userId).
 */
public record PartidaIniciadaEvent(String matchId, Map<String, List<String>> equipos) {
}
