package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.DemasiadosMensajesException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Anti-spam: cada usuario puede enviar pocos mensajes por ventana de tiempo, sumando todos los canales. */
class LimiteMensajes {

    static final int MAX_MENSAJES = 5;
    static final Duration VENTANA = Duration.ofSeconds(5);

    private final Map<String, Deque<Instant>> porUsuario = new ConcurrentHashMap<>();

    void registrar(String userId, Instant ahora) {
        porUsuario.compute(userId, (id, envios) -> {
            Deque<Instant> recientes = envios != null ? envios : new ArrayDeque<>();
            while (!recientes.isEmpty() && !recientes.peekFirst().isAfter(ahora.minus(VENTANA))) {
                recientes.removeFirst();
            }
            if (recientes.size() >= MAX_MENSAJES) throw new DemasiadosMensajesException();
            recientes.addLast(ahora);
            return recientes;
        });
    }
}
