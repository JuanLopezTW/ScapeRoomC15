package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import org.springframework.stereotype.Service;

/** Saca al jugador del mapa y avisa a su equipo. Es idempotente. */
@Service
public class DesconectarJugadorUseCase {

    private final JugadorEnMapaRepository jugadores;
    private final PosicionNotifier notifier;

    public DesconectarJugadorUseCase(JugadorEnMapaRepository jugadores, PosicionNotifier notifier) {
        this.jugadores = jugadores;
        this.notifier = notifier;
    }

    public void ejecutar(String matchId, String userId) {
        jugadores.remove(matchId, userId).ifPresent(jugador ->
                notifier.notificar(matchId, jugador.getEquipoId(), PosicionJugadorDto.desconectado(jugador)));
    }
}
