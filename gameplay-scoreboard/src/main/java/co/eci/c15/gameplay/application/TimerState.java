package co.eci.c15.gameplay.application;


public record TimerState(String matchId, long remainingSeconds, boolean timeUp) {
}