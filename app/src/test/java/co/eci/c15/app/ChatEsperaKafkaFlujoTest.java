package co.eci.c15.app;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Los mismos flujos de {@link ChatEsperaFlujoTest} con los eventos entre servicios por Kafka (como entre microservicios). */
@Testcontainers(disabledWithoutDocker = true)
class ChatEsperaKafkaFlujoTest extends ChatEsperaFlujoTest {

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry propiedades) {
        KafkaDePrueba.propiedades(propiedades, "chatEspera");
    }
}
