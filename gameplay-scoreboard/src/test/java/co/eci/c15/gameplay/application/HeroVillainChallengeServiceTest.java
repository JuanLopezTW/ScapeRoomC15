package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.ChallengeFinishedEvent;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HeroVillainChallengeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private TaskScheduler scheduler;
    private ScheduledFuture<?> future;
    private RolesRepository roles;
    private ChallengeNotifier notifier;
    private ApplicationEventPublisher events;
    private HeroVillainChallengeService service;

    @BeforeEach
    void setUp() {
        scheduler = mock(TaskScheduler.class);
        future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        doReturn(future).when(scheduler).scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));
        roles = new RolesRepositoryEnMemoria();
        notifier = mock(ChallengeNotifier.class);
        events = mock(ApplicationEventPublisher.class);
        RandomGenerator lowest = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public long nextLong(long bound) { return 0; }
        };
        service = new HeroVillainChallengeService(scheduler, Clock.fixed(NOW, ZoneOffset.UTC), roles, notifier,
                events, lowest, Duration.ofMinutes(30), Duration.ofSeconds(20));
        roles.saveIfAbsent(new AsignacionRoles("m1", "A", Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO)));
        roles.saveIfAbsent(new AsignacionRoles("m1", "B", Map.of("caro", Rol.HEROE, "dani", Rol.VERDUGO)));
    }

    private Runnable scheduledAt(Instant at) {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), eq(at));
        return task.getValue();
    }

    @Test
    void scheduleProgramsTheChallengeInsideTheWindowOnlyOnce() {
        service.schedule("m1", List.of("A", "B"));
        service.schedule("m1", List.of("A", "B"));

        scheduledAt(NOW.plus(Duration.ofMinutes(9)));
        assertFalse(service.isActive("m1"));
    }

    @Test
    void whenTheMomentArrivesTheChallengeStarts() {
        service.schedule("m1", List.of("A", "B"));
        scheduledAt(NOW.plus(Duration.ofMinutes(9))).run();

        assertTrue(service.isActive("m1"));
        ArgumentCaptor<ChallengeState> state = ArgumentCaptor.forClass(ChallengeState.class);
        verify(notifier).notify(state.capture());
        assertEquals(ChallengeState.STARTED, state.getValue().status());
        assertEquals(20, state.getValue().remainingSeconds());
        assertEquals(Map.of("A", 0L, "B", 0L), state.getValue().scores());
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), any(Instant.class),
                eq(HeroVillainChallengeService.BROADCAST_INTERVAL));
        verify(scheduler).schedule(any(Runnable.class), eq(NOW.plusSeconds(20)));
    }

    @Test
    void clicksGoToTheRunningChallenge() {
        service.schedule("m1", List.of("A", "B"));
        service.start("m1");

        service.click("m1", "ana");
        service.click("m1", "dani");

        assertEquals(Map.of("A", 0L, "B", 0L), service.find("m1").orElseThrow().scores());
    }

    @Test
    void clickWithoutChallengeIsRejected() {
        assertThrows(ChallengeNotActiveException.class, () -> service.click("m1", "ana"));
    }

    @Test
    void finishPublishesTheWinnerAndUnfreezesTheMap() {
        service.schedule("m1", List.of("A", "B"));
        service.start("m1");
        service.click("m1", "caro");

        ChallengeResult result = service.finish("m1").orElseThrow();

        assertEquals("B", result.winnerTeamId());
        assertFalse(service.isActive("m1"));
        verify(future, atLeast(2)).cancel(false);
        verify(events).publishEvent(new ChallengeFinishedEvent("m1", "B", Map.of("A", 0L, "B", 1L)));
        ArgumentCaptor<ChallengeState> state = ArgumentCaptor.forClass(ChallengeState.class);
        verify(notifier, times(2)).notify(state.capture());
        assertEquals(ChallengeState.FINISHED, state.getValue().status());
        assertEquals("B", state.getValue().winnerTeamId());
    }

    @Test
    void aTiePublishesNoWinner() {
        service.schedule("m1", List.of("A", "B"));
        service.start("m1");

        service.finish("m1");

        verify(events).publishEvent(new ChallengeFinishedEvent("m1", null, Map.of("A", 0L, "B", 0L)));
    }

    @Test
    void onlyOneChallengePerMatch() {
        service.schedule("m1", List.of("A", "B"));
        assertTrue(service.start("m1"));
        service.finish("m1");

        assertFalse(service.start("m1"));
        assertFalse(service.isActive("m1"));
    }

    @Test
    void startingEarlyCancelsTheScheduledOne() {
        service.schedule("m1", List.of("A", "B"));
        Runnable scheduled = scheduledAt(NOW.plus(Duration.ofMinutes(9)));
        service.start("m1");
        service.finish("m1");

        scheduled.run();

        verify(future, atLeastOnce()).cancel(false);
        assertFalse(service.isActive("m1"));
    }

    @Test
    void unknownMatchDoesNotStart() {
        assertFalse(service.start("no-existe"));
    }

    @Test
    void withoutRolesForTwoTeamsThereIsNoChallenge() {
        service.schedule("m2", List.of("X", "Y"));
        assertFalse(service.start("m2"));
        assertFalse(service.isActive("m2"));
        verifyNoInteractions(notifier);
    }

    @Test
    void whenTheMatchEndsTheChallengeIsDroppedOrClosed() {
        service.schedule("m1", List.of("A", "B"));
        service.matchOver("m1");
        verify(future).cancel(false);
        assertFalse(service.start("m1"));

        service.schedule("m3", List.of("A", "B"));
        roles.saveIfAbsent(new AsignacionRoles("m3", "A", Map.of("ana", Rol.HEROE)));
        roles.saveIfAbsent(new AsignacionRoles("m3", "B", Map.of("caro", Rol.HEROE)));
        service.start("m3");
        service.matchOver("m3");
        assertFalse(service.isActive("m3"));
        verify(events).publishEvent(any(ChallengeFinishedEvent.class));
    }
}
