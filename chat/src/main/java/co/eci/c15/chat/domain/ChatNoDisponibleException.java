package co.eci.c15.chat.domain;

/** La sala no existe o ya inicio la partida: el chat solo funciona en la sala de espera. */
public class ChatNoDisponibleException extends RuntimeException {
    public ChatNoDisponibleException(String salaId) {
        super("El chat solo está disponible en la sala de espera: " + salaId);
    }
}
