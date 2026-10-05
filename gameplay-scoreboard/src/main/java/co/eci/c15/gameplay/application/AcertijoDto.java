package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;

public record AcertijoDto(String id, String componenteMapaId, String enunciado, String estado) {
    public static AcertijoDto from(Acertijo a) {
        return new AcertijoDto(a.getId(), a.getComponenteMapaId(), a.getEnunciado(), a.getEstado().name());
    }
}
