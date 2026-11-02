package co.eci.c15.app.config;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompSeguridadInterceptorTest {

    private final JwtDecoder tokens = mock(JwtDecoder.class);
    private final EquipoRepository equipos = mock(EquipoRepository.class);
    private final MessageChannel canal = mock(MessageChannel.class);
    private StompSeguridadInterceptor interceptor;
    private Equipo equipo;

    @BeforeEach
    void setUp() {
        interceptor = new StompSeguridadInterceptor(tokens, equipos);
        equipo = Equipo.crear("m1", 1, 4);
        equipo.unirMiembro("auth0|ana");
        when(equipos.findById(equipo.getId())).thenReturn(Optional.of(equipo));
        when(equipos.findById("no-existe")).thenReturn(Optional.empty());
    }

    private static Message<byte[]> mensaje(StompHeaderAccessor stomp) {
        stomp.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], stomp.getMessageHeaders());
    }

    private static Message<byte[]> conectar(String autorizacion) {
        StompHeaderAccessor stomp = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (autorizacion != null) stomp.setNativeHeader("Authorization", autorizacion);
        return mensaje(stomp);
    }

    private static Message<byte[]> comando(StompCommand comando, String destino, String usuario) {
        StompHeaderAccessor stomp = StompHeaderAccessor.create(comando);
        stomp.setDestination(destino);
        if (usuario != null) stomp.setUser((Principal) () -> usuario);
        return mensaje(stomp);
    }

    private boolean pasa(Message<?> mensaje) {
        return interceptor.preSend(mensaje, canal) != null;
    }

    @Test
    void elConnectConTokenValidoIdentificaAlJugador() {
        when(tokens.decode("bueno")).thenReturn(Jwt.withTokenValue("bueno").header("alg", "RS256")
                .subject("auth0|ana").issuedAt(Instant.EPOCH).expiresAt(Instant.MAX).build());
        Message<byte[]> connect = conectar("Bearer bueno");

        interceptor.preSend(connect, canal);

        assertEquals("auth0|ana", StompHeaderAccessor.wrap(connect).getUser().getName());
    }

    @Test
    void elConnectSinTokenOConTokenInvalidoSeRechaza() {
        when(tokens.decode("malo")).thenThrow(new BadJwtException("firma invalida"));

        assertThrows(MessagingException.class, () -> interceptor.preSend(conectar(null), canal));
        assertThrows(MessagingException.class, () -> interceptor.preSend(conectar("otra-cosa"), canal));
        assertThrows(MessagingException.class, () -> interceptor.preSend(conectar("Bearer malo"), canal));
    }

    @Test
    void elClienteSoloPuedeEnviarAApp() {
        assertTrue(pasa(comando(StompCommand.SEND, "/app/salas/s1/sesion", "auth0|ana")));
        assertFalse(pasa(comando(StompCommand.SEND, "/topic/chat/global", "auth0|ana")));
        assertFalse(pasa(comando(StompCommand.SEND, "/topic/partidas/m1/reto", "auth0|ana")));
        assertFalse(pasa(comando(StompCommand.SEND, "/app/salas/s1/sesion", null)));
    }

    @Test
    void losTopicosDeUnEquipoSoloLosEscuchanSusMiembros() {
        String[] topicos = {
                "/topic/chat/partidas/m1/equipos/" + equipo.getId(),
                "/topic/partidas/m1/equipos/" + equipo.getId() + "/posiciones",
                "/topic/partidas/m1/equipos/" + equipo.getId() + "/inventario",
                "/topic/partidas/m1/equipos/" + equipo.getId() + "/roles",
        };
        for (String topico : topicos) {
            assertTrue(pasa(comando(StompCommand.SUBSCRIBE, topico, "auth0|ana")), topico);
            assertFalse(pasa(comando(StompCommand.SUBSCRIBE, topico, "auth0|rival")), topico);
        }
    }

    @Test
    void elEquipoDebeSerDeEsaPartida() {
        assertFalse(pasa(comando(StompCommand.SUBSCRIBE, "/topic/partidas/otra/equipos/" + equipo.getId() + "/roles", "auth0|ana")));
        assertFalse(pasa(comando(StompCommand.SUBSCRIBE, "/topic/partidas/m1/equipos/no-existe/roles", "auth0|ana")));
    }

    @Test
    void losTopicosGeneralesLosEscuchaCualquiera() {
        assertTrue(pasa(comando(StompCommand.SUBSCRIBE, "/topic/salas", "auth0|ana")));
        assertTrue(pasa(comando(StompCommand.SUBSCRIBE, "/topic/chat/global", "auth0|ana")));
        assertTrue(pasa(comando(StompCommand.SUBSCRIBE, "/topic/partidas/m1/reto", "auth0|rival")));
    }

    @Test
    void sinUsuarioNoSePuedeSuscribir() {
        assertFalse(pasa(comando(StompCommand.SUBSCRIBE, "/topic/salas", null)));
    }

    @Test
    void losDemasFramesPasan() {
        assertTrue(pasa(comando(StompCommand.UNSUBSCRIBE, null, "auth0|ana")));
        assertTrue(pasa(MessageBuilder.withPayload(new byte[0]).build()));
    }
}
