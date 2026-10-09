package co.eci.c15.common.events;

import java.util.Map;

/**
 * The match ended (HU-67.6). {@code winnerTeamId} is null on a draw; {@code scores} is the
 * final score of every team. {@code reason}: TIME_UP or ALL_PUZZLES_SOLVED.
 */
public record MatchFinishedEvent(String matchId, String reason, String winnerTeamId, Map<String, Integer> scores) {
}
