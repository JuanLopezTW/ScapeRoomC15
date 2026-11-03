package co.eci.c15.salas.infrastructure.events;

/**
 * Que jugadores tienen el lobby abierto en cada sala, sumando todas las instancias del back: un anfitrion
 * que recarga puede volver conectado a otra instancia y la que vio la desconexion debe enterarse.
 */
public interface PresenciaSala {

    void entrar(String sessionId, String salaId, String userId);

    void salir(String sessionId, String salaId, String userId);

    /** Si el jugador tiene al menos una conexion abierta en esa sala, en cualquier instancia. */
    boolean conectado(String salaId, String userId);
}
