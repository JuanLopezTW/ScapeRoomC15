package co.eci.c15.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** El servidor manda latidos pero no los exige: una pestaña en segundo plano no debe perder la conexion. */
@SpringBootTest
class WebSocketLatidosTest {

    @Autowired
    private SimpleBrokerMessageHandler broker;

    @Test
    void elServidorMandaLatidosSinExigirselosAlCliente() {
        assertArrayEquals(new long[]{10_000, 0}, broker.getHeartbeatValue());
        assertNotNull(broker.getTaskScheduler());
    }
}
