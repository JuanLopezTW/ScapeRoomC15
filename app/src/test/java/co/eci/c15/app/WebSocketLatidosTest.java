package co.eci.c15.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Sin latidos, una conexion quieta (esperando en el lobby) se cae y el back saca al jugador. */
@SpringBootTest
class WebSocketLatidosTest {

    @Autowired
    private SimpleBrokerMessageHandler broker;

    @Test
    void elBrokerNegociaLatidosParaQueLaConexionNoQuedeQuieta() {
        assertArrayEquals(new long[]{10_000, 10_000}, broker.getHeartbeatValue());
        assertNotNull(broker.getTaskScheduler());
    }
}
