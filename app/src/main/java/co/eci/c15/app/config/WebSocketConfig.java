package co.eci.c15.app.config;

import co.eci.c15.app.observabilidad.StompTrazaInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion STOMP unica para el monolito entero (gameplay, salas, chat). Con una instancia basta el broker
 * en memoria; con varias, {@code c15.stomp.broker.relay=true} pasa los topics por RabbitMQ.
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
@EnableConfigurationProperties(BrokerStompProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * El servidor manda latidos para que la conexion no quede quieta, pero no se los exige al cliente
     * (0): el navegador frena los timers de las pestañas en segundo plano y, si se exigieran, el
     * servidor cortaria a quien solo tiene la pestaña oculta.
     */
    private static final long[] LATIDO_MS = {10_000, 0};

    private final StompTrazaInterceptor traza;
    private final StompSeguridadInterceptor seguridad;
    private final BrokerStompProperties broker;

    public WebSocketConfig(@Lazy StompTrazaInterceptor traza, @Lazy StompSeguridadInterceptor seguridad,
                           BrokerStompProperties broker) {
        this.traza = traza;
        this.seguridad = seguridad;
        this.broker = broker;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(traza, seguridad, new LatidosClienteInterceptor());
        if (broker.relay()) registration.interceptors(DestinosRabbitInterceptor.haciaRabbit());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        if (broker.relay()) registration.interceptors(DestinosRabbitInterceptor.haciaCliente());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (broker.relay()) {
            // Cada cliente abre su propia conexion al broker con este usuario; el "sistema" es la que usa
            // el back para publicar (SimpMessagingTemplate). Ambas mandan y piden latidos al broker.
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(broker.host())
                    .setRelayPort(broker.puerto())
                    .setClientLogin(broker.usuario())
                    .setClientPasscode(broker.clave())
                    .setSystemLogin(broker.usuario())
                    .setSystemPasscode(broker.clave())
                    // Si no, Rabbit toma el "host" que mande el cliente (localhost) como virtual host
                    .setVirtualHost("/")
                    .setAutoStartup(broker.autoArranque());
            registry.configureBrokerChannel().interceptors(DestinosRabbitInterceptor.haciaRabbit());
        } else {
            registry.enableSimpleBroker("/topic")
                    .setHeartbeatValue(LATIDO_MS)
                    .setTaskScheduler(latidosStompScheduler());
        }
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public ThreadPoolTaskScheduler latidosStompScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-latidos-");
        return scheduler;
    }
}
