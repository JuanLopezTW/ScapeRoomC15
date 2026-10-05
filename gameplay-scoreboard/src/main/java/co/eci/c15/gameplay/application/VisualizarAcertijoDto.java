package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.ElementoInteractivo;

import java.util.List;

public record VisualizarAcertijoDto(
        String id,
        String enunciado,
        String estado,
        boolean resuelto,
        boolean puedeEnviarSolucion,
        List<ElementoDto> elementos
) {
    public record ElementoDto(String id, String tipo, String descripcion) {}

    public static VisualizarAcertijoDto from(Acertijo acertijo, List<ElementoInteractivo> elementos) {
        List<ElementoDto> dtos = elementos.stream()
                .map(e -> new ElementoDto(e.getId(), e.getTipo(), e.getDescripcion()))
                .toList();
        return new VisualizarAcertijoDto(
                acertijo.getId(),
                acertijo.getEnunciado(),
                acertijo.getEstado().name(),
                acertijo.isResuelto(),
                !acertijo.isResuelto(),
                dtos
        );
    }
}
