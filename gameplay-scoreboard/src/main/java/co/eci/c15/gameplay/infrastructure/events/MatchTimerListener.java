package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.MatchTimerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Starts the match timer when the match begins (HU-34.5 + HU-64.4). */
@Component
public class MatchTimerListener {

    private final MatchTimerService timers;
    private final Duration duration;

    public MatchTimerListener(MatchTimerService timers, @Value("${c15.partida.duracion:30m}") Duration duration) {
        this.timers = timers;
        this.duration = duration;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        timers.start(event.matchId(), duration);
    }
}
