package co.eci.c15.app.integracion;

import co.eci.c15.chat.application.SalaEsperaConsulta;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Component;

/**
 * Conecta el chat con las salas sin que un modulo dependa del otro: el chat pregunta por su
 * puerto {@link SalaEsperaConsulta} y aqui se responde con el repositorio de salas.
 */
@Component
public class SalaEsperaConsultaAdapter implements SalaEsperaConsulta {

    private final SalaRepository salas;

    public SalaEsperaConsultaAdapter(SalaRepository salas) {
        this.salas = salas;
    }

    @Override
    public boolean enEspera(String salaId) {
        return salas.findById(salaId).map(Sala::isDisponible).orElse(false);
    }

    @Override
    public boolean contieneJugador(String salaId, String userId) {
        return salas.findById(salaId).map(s -> s.contieneJugador(userId)).orElse(false);
    }
}
