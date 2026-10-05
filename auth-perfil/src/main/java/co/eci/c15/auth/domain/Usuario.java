package co.eci.c15.auth.domain;

import java.util.Objects;
import java.util.UUID;

public final class Usuario {

    private final String id;
    private final String username;

    private Usuario(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public static Usuario crear(String username) {
        validarUsername(username);
        return new Usuario(UUID.randomUUID().toString(), username);
    }

    public static void validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameInvalidoException("El nombre de usuario no puede estar vacío");
        }
        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            throw new UsernameInvalidoException("El nombre de usuario debe tener entre 3 y 20 caracteres alfanuméricos");
        }
    }

    public String getId() { return id; }
    public String getUsername() { return username; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario u)) return false;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
