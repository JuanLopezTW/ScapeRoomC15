package co.eci.c15.salas.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Equipo {

    private final String id;
    private final String salaId;
    private final int numero;
    private final int cupoMaximo;
    private final Set<String> miembros = new HashSet<>();

    public Equipo(String id, String salaId, int numero, int cupoMaximo) {
        this.id = Objects.requireNonNull(id);
        this.salaId = Objects.requireNonNull(salaId);
        this.numero = numero;
        this.cupoMaximo = cupoMaximo;
    }

    public static Equipo crear(String salaId, int numero, int cupoMaximo) {
        return new Equipo(UUID.randomUUID().toString(), salaId, numero, cupoMaximo);
    }

    public void unirMiembro(String userId) {
        if (miembros.size() >= cupoMaximo) throw new EquipoLlenoException(id);
        miembros.add(userId);
    }

    public String getId() { return id; }
    public String getSalaId() { return salaId; }
    public int getNumero() { return numero; }
    public int getCupoMaximo() { return cupoMaximo; }
    public Set<String> getMiembros() { return Collections.unmodifiableSet(miembros); }
    public boolean isFull() { return miembros.size() >= cupoMaximo; }
}
