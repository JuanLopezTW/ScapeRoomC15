package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Posicion;

/** Un jugador cambio de posicion en el mapa de su partida. */
public record PosicionActualizadaEvent(String matchId, String equipoId, String userId, Posicion posicion) {
}
