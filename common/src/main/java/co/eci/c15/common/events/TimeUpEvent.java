package co.eci.c15.common.events;

import java.time.Instant;


public record TimeUpEvent(String matchId, Instant occurredAt) {
}