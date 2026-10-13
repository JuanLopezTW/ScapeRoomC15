package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.ChatSalaRepository;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import co.eci.c15.chat.domain.MensajeChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * Chat de la sala de espera. Solo escriben y leen los jugadores de la sala, y solo mientras la
 * partida no ha iniciado; al iniciar (o si la sala ya no existe) el historial se borra.
 */
@Service
public class ChatEsperaService {

    private final ChatSalaRepository mensajes;
    private final SalaEsperaConsulta salas;
    private final ChatNotifier notifier;
    private final Clock clock;

    @Autowired
    public ChatEsperaService(ChatSalaRepository mensajes, SalaEsperaConsulta salas, ChatNotifier notifier) {
        this(mensajes, salas, notifier, Clock.systemUTC());
    }

    ChatEsperaService(ChatSalaRepository mensajes, SalaEsperaConsulta salas, ChatNotifier notifier, Clock clock) {
        this.mensajes = mensajes;
        this.salas = salas;
        this.notifier = notifier;
        this.clock = clock;
    }

    public MensajeChatDto enviar(String salaId, String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(salaId, userId, texto, clock.instant());
        validarAcceso(salaId, userId);
        mensajes.guardar(mensaje);
        MensajeChatDto dto = MensajeChatDto.from(mensaje);
        notifier.mensajeNuevo(dto);
        return dto;
    }

    public List<MensajeChatDto> historial(String salaId, String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
        validarAcceso(salaId, userId);
        return mensajes.historial(salaId).stream().map(MensajeChatDto::from).toList();
    }

    /** La sala salio de la espera (inicio la partida): el chat se cierra y se olvida. */
    public void cerrar(String salaId) {
        mensajes.borrar(salaId);
    }

    private void validarAcceso(String salaId, String userId) {
        if (!salas.enEspera(salaId)) {
            cerrar(salaId);
            throw new ChatNoDisponibleException(salaId);
        }
        if (!salas.contieneJugador(salaId, userId)) throw new JugadorFueraDeSalaException(salaId, userId);
    }
}
