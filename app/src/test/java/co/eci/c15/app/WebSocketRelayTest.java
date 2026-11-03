package co.eci.c15.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

/** Con el relay encendido los topics van a RabbitMQ y no queda el broker en memoria. */
@SpringBootTest(properties = {
        "c15.stomp.broker.relay=true",
        "c15.stomp.broker.host=rabbit-de-prueba",
        "c15.stomp.broker.puerto=61614",
        "c15.stomp.broker.usuario=c15",
        "c15.stomp.broker.clave=secreta",
        "c15.stomp.broker.auto-arranque=false"
})
class WebSocketRelayTest {

    @Autowired
    private StompBrokerRelayMessageHandler relay;

    @Autowired
    private ObjectProvider<SimpleBrokerMessageHandler> brokerEnMemoria;

    @Test
    void losTopicsVanAlBrokerExternoConSusCredenciales() {
        assertEquals("rabbit-de-prueba", relay.getRelayHost());
        assertEquals(61614, relay.getRelayPort());
        assertEquals("c15", relay.getClientLogin());
        assertEquals("secreta", relay.getClientPasscode());
        assertEquals("c15", relay.getSystemLogin());
        assertTrue(relay.getDestinationPrefixes().contains("/topic"));
        assertEquals("/", relay.getVirtualHost());
        assertFalse(relay.isAutoStartup());
    }

    @Test
    void noQuedaElBrokerEnMemoria() {
        assertNull(brokerEnMemoria.getIfAvailable());
    }
}
