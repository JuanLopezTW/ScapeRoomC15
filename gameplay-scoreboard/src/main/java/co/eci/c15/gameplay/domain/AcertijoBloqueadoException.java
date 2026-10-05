package co.eci.c15.gameplay.domain;

public class AcertijoBloqueadoException extends RuntimeException {
    public AcertijoBloqueadoException(String acertijoId) {
        super("El acertijo '" + acertijoId + "' está siendo resuelto por otro jugador");
    }
}
