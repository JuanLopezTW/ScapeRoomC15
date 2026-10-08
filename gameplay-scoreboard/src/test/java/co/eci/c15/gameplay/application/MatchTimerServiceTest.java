package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.TimeUpEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchTimerServiceTest {

    /** Reloj que se avanza a mano: el test no espera segundos reales. */
    static class MutableClock extends Clock {
        private volatile Instant now;
        MutableClock(Instant start) { this.now = start; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private TaskScheduler scheduler;
    private List<TimerState> sent;
    private List<Object> events;
    private MatchTimerService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-05T15:00:00Z"));
        scheduler = mock(TaskScheduler.class);
        doReturn(mock(ScheduledFuture.class)).when(scheduler)
                .scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));
        sent = new CopyOnWriteArrayList<>();
        events = new CopyOnWriteArrayList<>();
        service = new MatchTimerService(scheduler, clock, sent::add, events::add);
    }

    @Test
    void startingTwiceDoesNotDuplicateTheTimer() {
        var first = service.start("m1", Duration.ofSeconds(60));
        var second = service.start("m1", Duration.ofSeconds(60));
        assertSame(first, second);
        verify(scheduler, times(1))
                .scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));
    }

    @Test
    void findReturnsRemainingTime() {
        service.start("m1", Duration.ofSeconds(60));
        clock.advance(Duration.ofSeconds(10));
        assertEquals(50, service.find("m1").orElseThrow().remainingSeconds());
    }

    @Test
    void timeUpEventIsPublishedOnlyOnce() {
        service.start("m1", Duration.ofSeconds(5));
        clock.advance(Duration.ofSeconds(5));
        service.tick("m1");
        service.tick("m1");
        assertEquals(1, events.stream().filter(e -> e instanceof TimeUpEvent).count());
        assertTrue(service.find("m1").isEmpty());
    }

    @Test
    void concurrentStartsCreateASingleTimer() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                go.await();
                return service.start("m1", Duration.ofSeconds(60));
            });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        verify(scheduler, times(1))
                .scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));
    }
}
