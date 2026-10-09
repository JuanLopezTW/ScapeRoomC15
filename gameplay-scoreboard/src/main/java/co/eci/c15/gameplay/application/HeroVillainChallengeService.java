package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.ChallengeFinishedEvent;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.ChallengeTiming;
import co.eci.c15.gameplay.domain.HeroVillainChallenge;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.random.RandomGenerator;

/**
 * Lifecycle of the Hero vs Executioner challenge (HU-44.5): it fires once per match at a random
 * moment between 30% and 70% of the timer, lasts {@code c15.reto.duracion} (20s), broadcasts the
 * live scores and, when it ends, publishes {@link ChallengeFinishedEvent}. While it runs the map
 * is frozen (see {@link #isActive}).
 *
 * <p>Clicks don't take this service's lock: they go straight to the challenge, which is built
 * for concurrent clicks. Only start/finish/cancel are synchronized.
 */
@Service
public class HeroVillainChallengeService {

    private static final Logger log = LoggerFactory.getLogger(HeroVillainChallengeService.class);
    static final Duration BROADCAST_INTERVAL = Duration.ofMillis(200);

    private final TaskScheduler scheduler;
    private final Clock clock;
    private final RolesRepository roles;
    private final ChallengeNotifier notifier;
    private final ApplicationEventPublisher events;
    private final RandomGenerator random;
    private final Duration matchDuration;
    private final Duration challengeDuration;

    private final Map<String, HeroVillainChallenge> active = new ConcurrentHashMap<>();
    /** Guarded by {@code this}. */
    private final Map<String, Collection<String>> teamsByMatch = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingStarts = new HashMap<>();
    private final Map<String, List<ScheduledFuture<?>>> runningTasks = new HashMap<>();
    private final Set<String> done = new HashSet<>();

    public HeroVillainChallengeService(@Qualifier("gameplayScheduler") TaskScheduler scheduler,
                                       Clock clock,
                                       RolesRepository roles,
                                       ChallengeNotifier notifier,
                                       ApplicationEventPublisher events,
                                       @Qualifier("challengeRandom") RandomGenerator random,
                                       @Value("${c15.partida.duracion:30m}") Duration matchDuration,
                                       @Value("${c15.reto.duracion:20s}") Duration challengeDuration) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.roles = roles;
        this.notifier = notifier;
        this.events = events;
        this.random = random;
        this.matchDuration = matchDuration;
        this.challengeDuration = challengeDuration;
    }

    /** Programs the match's only challenge at a random moment of the 30%-70% window. */
    public synchronized void schedule(String matchId, Collection<String> teamIds) {
        if (teamsByMatch.containsKey(matchId)) return;
        teamsByMatch.put(matchId, List.copyOf(teamIds));
        Instant at = clock.instant().plus(ChallengeTiming.randomStart(matchDuration, random));
        pendingStarts.put(matchId, scheduler.schedule(() -> startScheduled(matchId), at));
    }

    /** Starts the challenge right away (also used by the dev endpoint). */
    public synchronized boolean start(String matchId) {
        ScheduledFuture<?> pending = pendingStarts.remove(matchId);
        if (pending != null) pending.cancel(false);
        if (done.contains(matchId) || active.containsKey(matchId)) return false;
        Collection<String> teams = teamsByMatch.get(matchId);
        if (teams == null) return false;

        Map<String, Map<String, Rol>> rolesByTeam = new LinkedHashMap<>();
        teams.forEach(team -> roles.findByEquipo(matchId, team)
                .map(AsignacionRoles::roles)
                .ifPresent(r -> rolesByTeam.put(team, r)));
        done.add(matchId);
        if (rolesByTeam.size() < 2) {
            log.warn("La partida {} no tiene roles para al menos dos equipos; no hay reto", matchId);
            return false;
        }

        Instant endsAt = clock.instant().plus(challengeDuration);
        HeroVillainChallenge challenge = new HeroVillainChallenge(matchId, rolesByTeam, endsAt);
        active.put(matchId, challenge);
        notifier.notify(state(challenge, ChallengeState.STARTED, null));

        List<ScheduledFuture<?>> tasks = new ArrayList<>();
        tasks.add(scheduler.scheduleAtFixedRate(() -> broadcast(matchId),
                clock.instant().plus(BROADCAST_INTERVAL), BROADCAST_INTERVAL));
        tasks.add(scheduler.schedule(() -> finish(matchId), endsAt));
        runningTasks.put(matchId, tasks);
        return true;
    }

    public void click(String matchId, String userId) {
        HeroVillainChallenge challenge = active.get(matchId);
        if (challenge == null) throw new ChallengeNotActiveException(matchId);
        challenge.click(userId, clock.instant());
    }

    /** While true the map is frozen: no moving, no puzzles, no picking up objects. */
    public boolean isActive(String matchId) {
        return active.containsKey(matchId);
    }

    public Optional<ChallengeState> find(String matchId) {
        return Optional.ofNullable(active.get(matchId)).map(c -> state(c, ChallengeState.RUNNING, null));
    }

    public synchronized Optional<ChallengeResult> finish(String matchId) {
        HeroVillainChallenge challenge = active.get(matchId);
        if (challenge == null) return Optional.empty();
        ChallengeResult result = challenge.finish();
        active.remove(matchId);
        Optional.ofNullable(runningTasks.remove(matchId)).ifPresent(tasks -> tasks.forEach(t -> t.cancel(false)));
        notifier.notify(new ChallengeState(matchId, ChallengeState.FINISHED, 0, result.scores(), result.winnerTeamId()));
        events.publishEvent(new ChallengeFinishedEvent(matchId, result.winnerTeamId(), result.scores()));
        return Optional.of(result);
    }

    /** The match is over: the pending challenge is dropped and a running one is closed. */
    public synchronized void matchOver(String matchId) {
        ScheduledFuture<?> pending = pendingStarts.remove(matchId);
        if (pending != null) pending.cancel(false);
        done.add(matchId);
        finish(matchId);
    }

    private void startScheduled(String matchId) {
        try {
            start(matchId);
        } catch (RuntimeException e) {
            log.error("No se pudo iniciar el reto de la partida {}", matchId, e);
        }
    }

    private void broadcast(String matchId) {
        try {
            HeroVillainChallenge challenge = active.get(matchId);
            if (challenge != null) notifier.notify(state(challenge, ChallengeState.RUNNING, null));
        } catch (RuntimeException e) {
            log.error("Error enviando el marcador del reto de la partida {}", matchId, e);
        }
    }

    private ChallengeState state(HeroVillainChallenge challenge, String status, String winner) {
        long ms = Duration.between(clock.instant(), challenge.getEndsAt()).toMillis();
        long remaining = ms <= 0 ? 0 : (ms + 999) / 1000;
        return new ChallengeState(challenge.getMatchId(), status, remaining, challenge.scores(), winner);
    }
}
