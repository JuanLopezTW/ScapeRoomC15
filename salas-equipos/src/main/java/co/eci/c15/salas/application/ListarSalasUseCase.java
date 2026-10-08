package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarSalasUseCase {

    private final SalaRepository salas;

    public ListarSalasUseCase(SalaRepository salas) {
        this.salas = salas;
    }

    public List<SalaDto> ejecutar() {
        return salas.findAll().stream().map(SalaDto::from).toList();
    }
}
