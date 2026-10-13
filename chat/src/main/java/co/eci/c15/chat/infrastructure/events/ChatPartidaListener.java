package co.eci.c15.chat.infrastructure.events;

import co.eci.c15.chat.application.ChatEsperaService;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al arrancar la partida la sala deja de estar en espera y su chat se cierra (matchId = salaId). */
@Component
public class ChatPartidaListener {

    private final ChatEsperaService chat;

    public ChatPartidaListener(ChatEsperaService chat) {
        this.chat = chat;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        chat.cerrar(event.matchId());
    }
}
