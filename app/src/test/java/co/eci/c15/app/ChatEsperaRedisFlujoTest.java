package co.eci.c15.app;

import co.eci.c15.chat.application.EquiposDePartida;
import co.eci.c15.chat.application.LimiteEnvios;
import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.infrastructure.estado.ChatRedis;
import co.eci.c15.chat.infrastructure.estado.EquiposDePartidaRedis;
import co.eci.c15.chat.infrastructure.estado.LimiteEnviosRedis;
import co.eci.c15.salas.infrastructure.estado.PresenciaSalaRedis;
import co.eci.c15.salas.infrastructure.events.PresenciaSala;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Los mismos flujos de chat de {@link ChatEsperaFlujoTest}, pero con el estado en un Redis real (como con
 * varias instancias). Sin Docker se salta.
 */
@Testcontainers(disabledWithoutDocker = true)
class ChatEsperaRedisFlujoTest extends ChatEsperaFlujoTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry propiedades) {
        RedisDePrueba.propiedades(propiedades);
    }

    @Autowired
    private ChatRepository historial;

    @Autowired
    private LimiteEnvios limite;

    @Autowired
    private EquiposDePartida equipos;

    @Autowired
    private PresenciaSala presencia;

    @Test
    void conRedisEncendidoSeUsanLosAdaptadoresDeRedis() {
        assertInstanceOf(ChatRedis.class, historial);
        assertInstanceOf(LimiteEnviosRedis.class, limite);
        assertInstanceOf(EquiposDePartidaRedis.class, equipos);
        assertInstanceOf(PresenciaSalaRedis.class, presencia);
    }
}
