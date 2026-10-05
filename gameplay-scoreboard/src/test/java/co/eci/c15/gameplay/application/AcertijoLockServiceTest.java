package co.eci.c15.gameplay.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AcertijoLockServiceTest {

    private AcertijoLockService service;

    @BeforeEach
    void setUp() {
        service = new AcertijoLockService();
    }

    @Test
    void primerJugadorObtieneLock() {
        assertTrue(service.bloquear("a1", "user-1"));
        assertFalse(service.estaBloqueadoPorOtro("a1", "user-1"));
    }

    @Test
    void segundoJugadorNoPuedeBloquearlo() {
        service.bloquear("a1", "user-1");
        assertFalse(service.bloquear("a1", "user-2"));
        assertTrue(service.estaBloqueadoPorOtro("a1", "user-2"));
    }

    @Test
    void mismoJugadorPuedeRebloquear() {
        service.bloquear("a1", "user-1");
        assertTrue(service.bloquear("a1", "user-1"));
    }

    @Test
    void liberarPermiteQueOtroBloquee() {
        service.bloquear("a1", "user-1");
        service.liberar("a1", "user-1");
        assertTrue(service.bloquear("a1", "user-2"));
    }

    @Test
    void liberarForzadoPorDesconexion() {
        service.bloquear("a1", "user-1");
        service.liberarForzado("a1");
        assertTrue(service.bloquear("a1", "user-2"));
    }

    @Test
    void soloUnJugadorGanaEnConcurrencia() throws InterruptedException {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final String userId = "user-" + i;
            pool.submit(() -> {
                try { go.await(); } catch (InterruptedException ignored) {}
                if (service.bloquear("a1", userId)) ganadores.incrementAndGet();
            });
        }

        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, ganadores.get());
    }
}
