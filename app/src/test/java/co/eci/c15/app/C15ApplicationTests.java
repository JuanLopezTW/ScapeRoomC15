package co.eci.c15.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto de Spring levante con TODOS los modulos
 * ensamblados (auth, salas, gameplay, chat, estadisticas).
 */
@SpringBootTest
class C15ApplicationTests {

    @Test
    void contextLoads() {
        // si el contexto no levanta, este test falla solo
    }
}
