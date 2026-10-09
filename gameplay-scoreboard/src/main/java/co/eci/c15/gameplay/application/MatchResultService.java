package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.AcertijoResueltoEvent;
import co.eci.c15.common.events.ChallengeFinishedEvent;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.MatchScoring;
import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * End of the match and its winner (HU-67.6). The match ends when time is up or as soon as a
 * team solves every puzzle of its map. Score = solved puzzles (HU-33.5) + challenges won
 * (HU-44.5); ties are broken by solved puzzles and otherwise it is a draw. Once it ends the map
 * stays frozen (see {@link #isFinished}).
 */
@Service
public class MatchResultService {

    private final ProgresoPartidaUseCase progress;
    private final ProgresoRepository progressRepository;
    private final HeroVillainChallengeService challenges;
    private final MatchTimerService timers;
    private final MatchResultNotifier notifier;
    private final ApplicationEventPublisher events;

    private final Map<String, List<String>> teamsByMatch = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> challengesWon = new ConcurrentHashMap<>();
    private final Map<String, MatchResult> results = new ConcurrentHashMap<>();

    public MatchResultService(ProgresoPartidaUseCase progress, ProgresoRepository progressRepository,
                              HeroVillainChallengeService challenges, MatchTimerService timers,
                              MatchResultNotifier notifier, ApplicationEventPublisher events) {
        this.progress = progress;
        this.progressRepository = progressRepository;
        this.challenges = challenges;
        this.timers = timers;
        this.notifier = notifier;
        this.events = events;
    }

    public void matchStarted(String matchId, Collection<String> teamIds) {
        teamsByMatch.putIfAbsent(matchId, teamIds.stream().sorted().toList());
    }

    public void challengeFinished(ChallengeFinishedEvent event) {
        if (event.winnerTeamId() == null) return;
        challengesWon.computeIfAbsent(event.matchId(), id -> new ConcurrentHashMap<>())
                .merge(event.winnerTeamId(), 1, Integer::sum);
    }

    /**
     * Counts the puzzle (idempotent, so it doesn't matter whether the progress listener ran
     * first) and ends the match if the team has now solved them all.
     */
    public void puzzleSolved(AcertijoResueltoEvent event) {
        if (!teamsByMatch.containsKey(event.matchId())) return;
        progress.acertijoResuelto(event);
        boolean completed = progressRepository.findByMatchId(event.matchId()).stream()
                .anyMatch(p -> p.getEquipoId().equals(event.equipoId()) && p.estaCompleto());
        if (completed) finish(event.matchId(), MatchResult.Reason.ALL_PUZZLES_SOLVED);
    }

    public void timeUp(String matchId) {
        finish(matchId, MatchResult.Reason.TIME_UP);
    }

    public synchronized Optional<MatchResult> finish(String matchId, MatchResult.Reason reason) {
        MatchResult existing = results.get(matchId);
        if (existing != null) return Optional.of(existing);
        List<String> teams = teamsByMatch.get(matchId);
        if (teams == null) return Optional.empty();

        // Closes a running challenge first so its winner is counted (publishes ChallengeFinishedEvent).
        challenges.matchOver(matchId);
        if (reason == MatchResult.Reason.ALL_PUZZLES_SOLVED) timers.stop(matchId);

        Map<String, Integer> puzzles = progressRepository.findByMatchId(matchId).stream()
                .collect(Collectors.toMap(ProgresoEquipo::getEquipoId, ProgresoEquipo::cantidadResueltos));
        MatchResult result = MatchScoring.rank(matchId, reason, teams, puzzles,
                challengesWon.getOrDefault(matchId, Map.of()));
        results.put(matchId, result);

        notifier.notify(result);
        Map<String, Integer> scores = new LinkedHashMap<>();
        result.standings().forEach(t -> scores.put(t.teamId(), t.score()));
        events.publishEvent(new MatchFinishedEvent(matchId, reason.name(), result.winnerTeamId(), scores));
        return Optional.of(result);
    }

    public boolean isFinished(String matchId) {
        return results.containsKey(matchId);
    }

    public Optional<MatchResult> find(String matchId) {
        return Optional.ofNullable(results.get(matchId));
    }
}
