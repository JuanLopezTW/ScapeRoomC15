package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.MatchResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lo que el resultado necesita de cada partida: sus equipos, los retos ganados y el resultado final. */
public interface ResultadosPartida {

    /** Se guarda la primera vez; repetirlo no cambia nada. */
    void guardarEquipos(String matchId, List<String> equipos);

    Optional<List<String>> equipos(String matchId);

    void sumarRetoGanado(String matchId, String equipoId);

    Map<String, Integer> retosGanados(String matchId);

    /** @return false si la partida ya tenia resultado: termina una sola vez */
    boolean guardar(MatchResult resultado);

    Optional<MatchResult> find(String matchId);
}
