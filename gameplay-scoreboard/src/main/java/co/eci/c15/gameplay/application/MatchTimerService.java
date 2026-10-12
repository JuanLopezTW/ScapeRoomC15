package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.TimeUpEvent;
import co.eci.c15.gameplay.domain.MatchTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class MatchTimerService {

    private static final Logger log = LoggerFactory.getLogger(MatchTimerService.class);
    private static final Duration TICK_INTERVAL = Duration.ofSeconds(1);

    private final Map<String, MatchTimer> timers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private final TaskScheduler scheduler;
    private final Clock clock;
    private final TimerNotifier notifier;
    private final ApplicationEventPublisher events;

    public MatchTimerService(@Qualifier("gameplayScheduler") TaskScheduler scheduler,
                             Clock clock,
                             TimerNotifier notifier,
                             ApplicationEventPublisher events) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.notifier = notifier;
        this.events = events;
    }
    public MatchTimer start(String matchId, Duration duration) {
        MatchTimer created = new MatchTimer(matchId, clock.instant(), duration);
        MatchTimer existing = timers.putIfAbsent(matchId, created);
        if (existing != null) {
            return existing;
        }
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> tick(matchId),
                clock.instant().plus(TICK_INTERVAL),
                TICK_INTERVAL);
        tasks.put(matchId, task);
        notifier.notify(stateOf(created, clock.instant()));
        return created;
    }

    /** Stops the timer without publishing {@link TimeUpEvent} (the match ended early). */
    public boolean stop(String matchId) {
        MatchTimer timer = timers.remove(matchId);
        if (timer == null) {
            return false;
        }
        ScheduledFuture<?> task = tasks.remove(matchId);
        if (task != null) {
            task.cancel(false);
        }
        return true;
    }

    public Optional<TimerState> find(String matchId) {
        return Optional.ofNullable(timers.get(matchId))
                .map(timer -> stateOf(timer, clock.instant()));
    }

    void tick(String matchId) {
        try {
            MatchTimer timer = timers.get(matchId);
            if (timer == null) {
                return;
            }
            Instant now = clock.instant();
            notifier.notify(stateOf(timer, now));
            if (timer.isTimeUp(now) && timers.remove(matchId, timer)) {
                ScheduledFuture<?> task = tasks.remove(matchId);
                if (task != null) {
                    task.cancel(false);
                }
                events.publishEvent(new TimeUpEvent(matchId, now));
            }
        } catch (RuntimeException e) {
            log.error("Error en el tick del cronometro de la partida {}", matchId, e);
        }
    }

    private TimerState stateOf(MatchTimer timer, Instant now) {
        return new TimerState(timer.getMatchId(), timer.remainingSeconds(now), timer.isTimeUp(now));
    }
}