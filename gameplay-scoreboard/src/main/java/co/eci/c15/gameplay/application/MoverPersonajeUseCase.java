package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class MoverPersonajeUseCase {

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;
    private final ApplicationEventPublisher events;

    public MoverPersonajeUseCase(JugadorEnMapaRepository jugadores,
                                 ObtenerMapaUseCase obtenerMapa,
                                 ApplicationEventPublisher events) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
        this.events = events;
    }

    public MovimientoDto ejecutar(String matchId, String userId, Posicion destino) {
        JugadorEnMapa jugador = jugadores.find(matchId, userId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
        MapaIsometrico mapa = obtenerMapa.obtener(matchId);
        JugadorEnMapa.Movimiento movimiento = jugador.mover(mapa, destino);
        events.publishEvent(new PosicionActualizadaEvent(matchId, jugador.getEquipoId(), userId, movimiento.hasta()));
        return MovimientoDto.from(userId, movimiento);
    }
}
