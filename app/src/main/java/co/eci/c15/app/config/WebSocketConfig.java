package co.eci.c15.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion STOMP unica para todo el monolito (gameplay, salas, chat).
 *
 * Topics acordados:
 *   /topic/salas                                   catalogo de salas
 *   /topic/salas/{id}                              detalle de la sala (lobby, listos)
 *   /topic/salas/{id}/inicio                       cuenta regresiva y arranque
 *   /topic/partidas/{id}/cronometro                tiempo restante
 *   /topic/partidas/{id}/reto                      reto Heroe vs Verdugo
 *   /topic/partidas/{id}/resultado                 fin de partida
 *   /topic/partidas/{id}/progreso                  acertijos resueltos por equipo
 *   /topic/partidas/{id}/equipos/{eq}/posiciones   posiciones del equipo
 *   /topic/partidas/{id}/equipos/{eq}/inventario   inventario del equipo
 *   /topic/partidas/{id}/equipos/{eq}/roles        roles del equipo
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
