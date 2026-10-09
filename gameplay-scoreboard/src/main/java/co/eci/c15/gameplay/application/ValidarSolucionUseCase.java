package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComparadorRespuestas;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.SolucionAcertijoRepository;
import co.eci.c15.gameplay.domain.SolucionNoDisponibleException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validacion de la solucion de un acertijo (HU-29.5). Quien lo tiene abierto envia una
 * respuesta, o se toma lo que ingreso con clicks. Si es correcta el acertijo queda resuelto
 * para todo su equipo, el jugador queda libre para moverse y se publica el evento de
 * acertijo resuelto; si no, se descarta lo ingresado y puede volver a intentar.
 */
@Service
public class ValidarSolucionUseCase {

    static final int MAX_PASOS = 100;
    static final int MAX_LONGITUD_PASO = 50;

    private final JugadorEnMapaRepository jugadores;
    private final AcertijoRepository acertijos;
    private final SolucionAcertijoRepository soluciones;
    private final AcertijoEnPartidaRepository estados;
    private final ApplicationEventPublisher events;

    public ValidarSolucionUseCase(JugadorEnMapaRepository jugadores, AcertijoRepository acertijos,
                                  SolucionAcertijoRepository soluciones, AcertijoEnPartidaRepository estados,
                                  ApplicationEventPublisher events) {
        this.jugadores = jugadores;
        this.acertijos = acertijos;
        this.soluciones = soluciones;
        this.estados = estados;
        this.events = events;
    }

    /** @param respuesta lo que envia el jugador; si es null se valida lo ingresado con clicks */
    public ResultadoSolucionDto ejecutar(String matchId, String componenteMapaId, String userId, List<String> respuesta) {
        validar(respuesta);
        JugadorEnMapa jugador = jugadores.find(matchId, userId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
        acertijos.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        List<String> solucion = soluciones.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new SolucionNoDisponibleException(componenteMapaId));
        AcertijoEnPartida estado = estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId);

        AcertijoEnPartida.Resultado resultado =
                estado.intentarResolver(userId, respuesta, candidata -> ComparadorRespuestas.coincide(candidata, solucion));

        if (resultado == AcertijoEnPartida.Resultado.INCORRECTA) return ResultadoSolucionDto.error();
        jugador.cerrarAcertijo(componenteMapaId);
        events.publishEvent(new AcertijoResueltoEvent(matchId, jugador.getEquipoId(), componenteMapaId, userId));
        return ResultadoSolucionDto.acierto();
    }

    private static void validar(List<String> respuesta) {
        if (respuesta == null) return;
        if (respuesta.size() > MAX_PASOS) {
            throw new IllegalArgumentException("La respuesta no puede tener mas de " + MAX_PASOS + " pasos");
        }
        for (String paso : respuesta) {
            if (paso == null || paso.length() > MAX_LONGITUD_PASO) {
                throw new IllegalArgumentException("Cada paso de la respuesta debe tener hasta " + MAX_LONGITUD_PASO + " caracteres");
            }
        }
    }
}
