package co.eci.c15.chat.infrastructure.events;

import co.eci.c15.chat.application.ChatEsperaService;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class ChatPartidaListenerTest {

    @Test
    void alIniciarLaPartidaSeCierraElChatDeLaSala() {
        ChatEsperaService chat = mock(ChatEsperaService.class);

        new ChatPartidaListener(chat).onPartidaIniciada(new PartidaIniciadaEvent("s1", Map.of("e1", List.of("ana"))));

        verify(chat).cerrar("s1");
    }
}
