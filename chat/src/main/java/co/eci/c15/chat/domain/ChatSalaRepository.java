package co.eci.c15.chat.domain;

import java.util.List;

/** Historial del chat de cada sala de espera. Solo guarda los ultimos mensajes. */
public interface ChatSalaRepository {
    void guardar(MensajeChat mensaje);

    /** Mensajes de la sala, del mas viejo al mas nuevo. */
    List<MensajeChat> historial(String salaId);

    void borrar(String salaId);
}
