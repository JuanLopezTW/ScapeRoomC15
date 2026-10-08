package co.eci.c15.gameplay.domain;

import java.util.List;
import java.util.Objects;

/**
 * Personaje de un jugador sobre el mapa. El cambio de posicion es atomico: dos
 * movimientos simultaneos del mismo jugador se serializan y cada uno parte de la
 * posicion real en que quedo el anterior.
 */
public final class JugadorEnMapa {

    public record Movimiento(Posicion desde, Posicion hasta, List<Posicion> ruta) {}

    private final String matchId;
    private final String equipoId;
    private final String userId;
    private Posicion posicion;

    public JugadorEnMapa(String matchId, String equipoId, String userId, Posicion posicionInicial) {
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
        this.userId = Objects.requireNonNull(userId);
        this.posicion = Objects.requireNonNull(posicionInicial);
    }

    public synchronized Movimiento mover(MapaIsometrico mapa, Posicion destino) {
        if (!mapa.dentro(destino)) throw new MovimientoInvalidoException("El destino esta fuera del mapa");
        if (!mapa.esTransitable(destino)) throw new MovimientoInvalidoException("El destino no es transitable");
        List<Posicion> ruta = mapa.rutaEntre(posicion, destino)
                .orElseThrow(() -> new MovimientoInvalidoException("No hay un camino hasta el destino"));
        Posicion desde = posicion;
        posicion = destino;
        return new Movimiento(desde, destino, ruta);
    }

    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
    public String getUserId() { return userId; }
    public synchronized Posicion getPosicion() { return posicion; }
}
