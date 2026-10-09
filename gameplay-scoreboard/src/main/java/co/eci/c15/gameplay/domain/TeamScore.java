package co.eci.c15.gameplay.domain;

/** A team's final score: {@code score = puzzlesSolved + challengesWon}. */
public record TeamScore(String teamId, int puzzlesSolved, int challengesWon, int score) {
}
