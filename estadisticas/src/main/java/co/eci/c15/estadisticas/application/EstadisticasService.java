package co.eci.c15.estadisticas.application;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.EstadisticasRepository;
import co.eci.c15.estadisticas.domain.PartidaJugada;
import co.eci.c15.estadisticas.domain.ResultadoPartida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra el resultado de cada jugador cuando termina una partida y arma el ranking. Solo escucha
 * eventos: los equipos llegan al iniciar la partida porque el evento de fin solo trae puntajes.
 */
@Service
public class EstadisticasService {

    static final int MAX_RANKING = 50;
    static final int ULTIMAS_PARTIDAS = 5;

    private final EstadisticasRepository repositorio;
    private final Clock clock;
    private final Map<String, Map<String, List<String>>> equiposPorPartida = new ConcurrentHashMap<>();

    @Autowired
    public EstadisticasService(EstadisticasRepository repositorio) {
        this(repositorio, Clock.systemUTC());
    }

    EstadisticasService(EstadisticasRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public void partidaIniciada(String matchId, Map<String, List<String>> equipos) {
        equiposPorPartida.put(matchId, equipos);
    }

    @Transactional
    public void partidaTerminada(MatchFinishedEvent evento) {
        Map<String, List<String>> equipos = equiposPorPartida.remove(evento.matchId());
        if (equipos == null || repositorio.existePartida(evento.matchId())) return;

        Instant ahora = clock.instant();
        int mejorPuntaje = evento.scores().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<PartidaJugada> partidas = new ArrayList<>();
        equipos.forEach((equipoId, jugadores) -> {
            int puntaje = evento.scores().getOrDefault(equipoId, 0);
            ResultadoPartida resultado = resultadoDe(equipoId, puntaje, mejorPuntaje, evento.winnerTeamId());
            jugadores.forEach(userId ->
                    partidas.add(new PartidaJugada(evento.matchId(), userId, equipoId, resultado, puntaje, ahora)));
        });
        repositorio.guardarTodas(partidas);
    }

    public List<JugadorRankingDto> ranking(int limite) {
        List<EstadisticaJugador> mejores = repositorio.ranking(Math.max(1, Math.min(limite, MAX_RANKING)));
        List<JugadorRankingDto> ranking = new ArrayList<>();
        for (int i = 0; i < mejores.size(); i++) ranking.add(JugadorRankingDto.from(i + 1, mejores.get(i)));
        return Collections.unmodifiableList(ranking);
    }

    public EstadisticasJugadorDto deJugador(String userId) {
        EstadisticaJugador totales = repositorio.deJugador(userId).orElse(EstadisticaJugador.sinPartidas(userId));
        return EstadisticasJugadorDto.from(totales, repositorio.ultimasDeJugador(userId, ULTIMAS_PARTIDAS));
    }

    /** Sin ganador, empatan los que llegaron al mejor puntaje; los demas pierden. */
    private static ResultadoPartida resultadoDe(String equipoId, int puntaje, int mejorPuntaje, String ganador) {
        if (ganador != null) return ganador.equals(equipoId) ? ResultadoPartida.VICTORIA : ResultadoPartida.DERROTA;
        return puntaje == mejorPuntaje ? ResultadoPartida.EMPATE : ResultadoPartida.DERROTA;
    }
}
