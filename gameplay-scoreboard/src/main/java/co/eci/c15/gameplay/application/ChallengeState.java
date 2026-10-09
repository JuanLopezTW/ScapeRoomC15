package co.eci.c15.gameplay.application;

import java.util.Map;

/**
 * What players see of the challenge. {@code status}: STARTED, RUNNING (live scores) or FINISHED
 * (with {@code winnerTeamId}, null on a tie).
 */
public record ChallengeState(String matchId, String status, long remainingSeconds,
                             Map<String, Long> scores, String winnerTeamId) {

    public static final String STARTED = "STARTED";
    public static final String RUNNING = "RUNNING";
    public static final String FINISHED = "FINISHED";
}
