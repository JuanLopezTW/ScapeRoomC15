package co.eci.c15.gameplay.domain;

import java.util.Map;
import java.util.Optional;

/** Final result of the Hero vs Executioner challenge. {@code winnerTeamId} is null on a tie. */
public record ChallengeResult(String matchId, String winnerTeamId, Map<String, Long> scores) {

    public ChallengeResult {
        scores = Map.copyOf(scores);
    }

    public Optional<String> winner() {
        return Optional.ofNullable(winnerTeamId);
    }
}
