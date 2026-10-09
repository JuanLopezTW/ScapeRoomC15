package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;

import java.util.Set;

public record EquipoDto(String id, String salaId, int numero, int cupoMaximo, Set<String> miembros) {
    public static EquipoDto from(Equipo equipo) {
        return new EquipoDto(equipo.getId(), equipo.getSalaId(), equipo.getNumero(),
                equipo.getCupoMaximo(), equipo.getMiembros());
    }
}
