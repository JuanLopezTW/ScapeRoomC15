package co.eci.c15.gameplay.domain;

import java.util.Objects;

public final class Acertijo {

    public enum Estado { PENDIENTE, RESUELTO }

    private final String id;
    private final String componenteMapaId;
    private final String enunciado;
    private Estado estado;

    public Acertijo(String id, String componenteMapaId, String enunciado) {
        this.id = Objects.requireNonNull(id);
        this.componenteMapaId = Objects.requireNonNull(componenteMapaId);
        this.enunciado = Objects.requireNonNull(enunciado);
        this.estado = Estado.PENDIENTE;
    }

    public void marcarResuelto() { this.estado = Estado.RESUELTO; }

    public boolean isResuelto() { return estado == Estado.RESUELTO; }

    public String getId() { return id; }
    public String getComponenteMapaId() { return componenteMapaId; }
    public String getEnunciado() { return enunciado; }
    public Estado getEstado() { return estado; }
}
