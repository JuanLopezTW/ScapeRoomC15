package co.eci.c15.chat.infrastructure.persistence;

import co.eci.c15.chat.domain.ChatSalaRepository;
import co.eci.c15.chat.domain.MensajeChat;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** El chat de espera es efimero: vive en memoria y solo guarda los ultimos mensajes de cada sala. */
@Repository
public class ChatSalaEnMemoria implements ChatSalaRepository {

    public static final int MAX_MENSAJES = 50;

    private final Map<String, Deque<MensajeChat>> porSala = new ConcurrentHashMap<>();

    @Override
    public void guardar(MensajeChat mensaje) {
        porSala.compute(mensaje.salaId(), (id, cola) -> {
            Deque<MensajeChat> c = cola != null ? cola : new ArrayDeque<>();
            synchronized (c) {
                c.addLast(mensaje);
                while (c.size() > MAX_MENSAJES) c.removeFirst();
            }
            return c;
        });
    }

    @Override
    public List<MensajeChat> historial(String salaId) {
        Deque<MensajeChat> cola = porSala.get(salaId);
        if (cola == null) return List.of();
        synchronized (cola) {
            return List.copyOf(cola);
        }
    }

    @Override
    public void borrar(String salaId) {
        porSala.remove(salaId);
    }
}
