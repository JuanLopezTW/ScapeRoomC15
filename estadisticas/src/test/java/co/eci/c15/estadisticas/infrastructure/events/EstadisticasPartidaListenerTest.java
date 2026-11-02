package co.eci.c15.estadisticas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.estadisticas.application.EstadisticasService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EstadisticasPartidaListenerTest {

    private final EstadisticasService estadisticas = mock(EstadisticasService.class);
    private final EstadisticasPartidaListener listener = new EstadisticasPartidaListener(estadisticas);
    private final MatchFinishedEvent fin = new MatchFinishedEvent("m1", "TIME_UP", null, Map.of());

    @Test
    void alIniciarGuardaLosEquipos() {
        Map<String, List<String>> equipos = Map.of("e1", List.of("ana"));

        listener.onPartidaIniciada(new PartidaIniciadaEvent("m1", equipos));

        verify(estadisticas).partidaIniciada("m1", equipos);
    }

    @Test
    void alTerminarRegistraElResultado() {
        listener.onPartidaTerminada(fin);

        verify(estadisticas).partidaTerminada(fin);
    }

    @Test
    void unFalloAlGuardarNoLeDañaElFinDePartidaALosDemasModulos() {
        doThrow(new IllegalStateException("sin base de datos")).when(estadisticas).partidaTerminada(fin);

        assertDoesNotThrow(() -> listener.onPartidaTerminada(fin));
    }
}
