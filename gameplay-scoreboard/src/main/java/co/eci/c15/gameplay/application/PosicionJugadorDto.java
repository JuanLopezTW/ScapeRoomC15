package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;

/** Posicion de un jugador; conectado=false avisa que el jugador salio del mapa. */
public record PosicionJugadorDto(String userId, int x, int y, boolean conectado) {

    public static PosicionJugadorDto conectado(String userId, Posicion p) {
        return new PosicionJugadorDto(userId, p.x(), p.y(), true);
    }

    public static PosicionJugadorDto desconectado(JugadorEnMapa jugador) {
        Posicion p = jugador.getPosicion();
        return new PosicionJugadorDto(jugador.getUserId(), p.x(), p.y(), false);
    }
}
