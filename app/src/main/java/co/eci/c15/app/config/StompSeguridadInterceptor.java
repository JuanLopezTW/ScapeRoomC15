package co.eci.c15.app.config;

import co.eci.c15.salas.domain.EquipoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Seguridad del WebSocket: el CONNECT debe traer un token valido, los clientes solo pueden mandar a
 * /app (si no, podrian publicar directo en un /topic y falsificar mensajes) y los topics de un equipo
 * solo los escuchan sus miembros. Lo denegado se descarta sin tumbar la conexion.
 */
@Component
public class StompSeguridadInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompSeguridadInterceptor.class);
    private static final Pattern TOPICO_DE_EQUIPO =
            Pattern.compile("^/topic/(?:chat/)?partidas/([^/]+)/equipos/([^/]+)(?:/.*)?$");
    private static final String PREFIJO_TOKEN = "Bearer ";

    private final JwtDecoder tokens;
    private final EquipoRepository equipos;

    public StompSeguridadInterceptor(JwtDecoder tokens, EquipoRepository equipos) {
        this.tokens = tokens;
        this.equipos = equipos;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (stomp == null || stomp.getCommand() == null) return message;

        StompCommand comando = stomp.getCommand();
        if (comando == StompCommand.CONNECT) {
            JwtAuthenticationToken usuario = new JwtAuthenticationToken(validarToken(stomp));
            stomp.setUser(usuario);
            log.atInfo().addKeyValue("evento", "stomp.conectado").addKeyValue("userId", usuario.getName())
                    .log("STOMP conectado: {}", usuario.getName());
            return message;
        }
        if (comando == StompCommand.SEND && !permitidoEnviar(stomp)) return denegar(stomp);
        if (comando == StompCommand.SUBSCRIBE && !permitidoSuscribir(stomp)) return denegar(stomp);
        return message;
    }

    private Jwt validarToken(StompHeaderAccessor stomp) {
        String autorizacion = stomp.getFirstNativeHeader("Authorization");
        if (autorizacion == null || !autorizacion.startsWith(PREFIJO_TOKEN)) {
            throw new MessagingException("Falta el token");
        }
        try {
            return tokens.decode(autorizacion.substring(PREFIJO_TOKEN.length()));
        } catch (JwtException e) {
            throw new MessagingException("Token invalido", e);
        }
    }

    private static boolean permitidoEnviar(StompHeaderAccessor stomp) {
        String destino = stomp.getDestination();
        return stomp.getUser() != null && destino != null && destino.startsWith("/app/");
    }

    private boolean permitidoSuscribir(StompHeaderAccessor stomp) {
        if (stomp.getUser() == null || stomp.getDestination() == null) return false;
        Matcher equipo = TOPICO_DE_EQUIPO.matcher(stomp.getDestination());
        if (!equipo.matches()) return true;
        String userId = stomp.getUser().getName();
        return equipos.findById(equipo.group(2))
                .filter(e -> e.getSalaId().equals(equipo.group(1))) // matchId = salaId
                .filter(e -> e.contieneMiembro(userId))
                .isPresent();
    }

    private static Message<?> denegar(StompHeaderAccessor stomp) {
        log.warn("{} denegado a {} para {}", stomp.getCommand(), stomp.getDestination(),
                stomp.getUser() != null ? stomp.getUser().getName() : "sin usuario");
        return null;
    }
}
