package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import co.eci.c15.chat.domain.MensajeChat;
import co.eci.c15.chat.infrastructure.persistence.ChatSalaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatEsperaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T15:00:00Z");

    private SalasFalsas salas;
    private ChatSalaEnMemoria mensajes;
    private ChatNotifier notifier;
    private ChatEsperaService chat;

    @BeforeEach
    void setUp() {
        salas = new SalasFalsas();
        salas.enEspera("s1", "ana", "beto");
        mensajes = new ChatSalaEnMemoria();
        notifier = mock(ChatNotifier.class);
        chat = new ChatEsperaService(mensajes, salas, notifier, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void unJugadorDeLaSalaEnviaYSeAvisaATodos() {
        MensajeChatDto dto = chat.enviar("s1", "ana", " hola ");

        assertEquals("s1", dto.salaId());
        assertEquals("ana", dto.userId());
        assertEquals("hola", dto.texto());
        assertEquals(AHORA, dto.enviadoEn());
        verify(notifier).mensajeNuevo(dto);
        assertEquals(List.of(dto), chat.historial("s1", "beto"));
    }

    @Test
    void elHistorialVieneEnOrdenDeEnvio() {
        chat.enviar("s1", "ana", "uno");
        chat.enviar("s1", "beto", "dos");
        chat.enviar("s1", "ana", "tres");

        assertEquals(List.of("uno", "dos", "tres"),
                chat.historial("s1", "ana").stream().map(MensajeChatDto::texto).toList());
    }

    @Test
    void elHistorialDeUnaSalaSinMensajesEstaVacio() {
        assertTrue(chat.historial("s1", "ana").isEmpty());
    }

    @Test
    void cadaSalaTieneSuPropioChat() {
        salas.enEspera("s2", "caro");
        chat.enviar("s1", "ana", "en s1");
        chat.enviar("s2", "caro", "en s2");

        assertEquals(List.of("en s1"), chat.historial("s1", "ana").stream().map(MensajeChatDto::texto).toList());
        assertEquals(List.of("en s2"), chat.historial("s2", "caro").stream().map(MensajeChatDto::texto).toList());
    }

    @Test
    void quienNoEstaEnLaSalaNoPuedeEscribirNiLeer() {
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.enviar("s1", "intruso", "hola"));
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.historial("s1", "intruso"));
        verifyNoInteractions(notifier);
    }

    @Test
    void conLaPartidaIniciadaElChatYaNoEstaDisponibleYSeBorra() {
        chat.enviar("s1", "ana", "antes de jugar");
        salas.iniciarPartida("s1");

        assertThrows(ChatNoDisponibleException.class, () -> chat.enviar("s1", "ana", "durante"));
        assertThrows(ChatNoDisponibleException.class, () -> chat.historial("s1", "ana"));
        assertTrue(mensajes.historial("s1").isEmpty());
        verify(notifier, times(1)).mensajeNuevo(any());
    }

    @Test
    void unaSalaQueNoExisteNoTieneChat() {
        assertThrows(ChatNoDisponibleException.class, () -> chat.enviar("no-existe", "ana", "hola"));
        assertThrows(ChatNoDisponibleException.class, () -> chat.historial("no-existe", "ana"));
    }

    @Test
    void unMensajeInvalidoNoSeGuardaNiSeAvisa() {
        assertThrows(IllegalArgumentException.class, () -> chat.enviar("s1", "ana", "   "));
        assertThrows(IllegalArgumentException.class, () -> chat.enviar("s1", "ana", "x".repeat(MensajeChat.MAX_LONGITUD + 1)));

        assertTrue(chat.historial("s1", "ana").isEmpty());
        verifyNoInteractions(notifier);
    }

    @Test
    void elHistorialPideUserId() {
        assertThrows(IllegalArgumentException.class, () -> chat.historial("s1", null));
        assertThrows(IllegalArgumentException.class, () -> chat.historial("s1", " "));
    }

    @Test
    void cerrarBorraElHistorial() {
        chat.enviar("s1", "ana", "hola");

        chat.cerrar("s1");

        assertTrue(chat.historial("s1", "ana").isEmpty());
    }

    /** Salas en espera con sus jugadores; las que no estan aqui no existen. */
    static class SalasFalsas implements SalaEsperaConsulta {
        private final Map<String, Set<String>> jugadores = new HashMap<>();
        private final Set<String> iniciadas = new HashSet<>();

        void enEspera(String salaId, String... usuarios) {
            jugadores.put(salaId, Set.of(usuarios));
        }

        void iniciarPartida(String salaId) {
            iniciadas.add(salaId);
        }

        @Override
        public boolean enEspera(String salaId) {
            return jugadores.containsKey(salaId) && !iniciadas.contains(salaId);
        }

        @Override
        public boolean contieneJugador(String salaId, String userId) {
            return jugadores.getOrDefault(salaId, Set.of()).contains(userId);
        }
    }
}
