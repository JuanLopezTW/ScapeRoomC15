package co.eci.c15.gameplay.domain;

import java.util.List;

/**
 * How a match ended. {@code winnerTeamId} is null on a draw, and then {@code tiedTeamIds} lists
 * the teams that tied at the top. {@code standings} goes from first to last.
 */
public record MatchResult(String matchId, Reason reason, String winnerTeamId,
                          List<String> tiedTeamIds, List<TeamScore> standings) {

    public enum Reason { TIME_UP, ALL_PUZZLES_SOLVED }

    public MatchResult {
        tiedTeamIds = List.copyOf(tiedTeamIds);
        standings = List.copyOf(standings);
    }

    public boolean isDraw() {
        return winnerTeamId == null;
    }
}
