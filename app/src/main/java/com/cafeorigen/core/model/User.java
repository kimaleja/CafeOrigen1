package com.cafeorigen.core.model;

import androidx.annotation.NonNull;

/**
 * CafeOrigen — Modelo de usuario (mock local, sin backend).
 * Inmutable y compartido por los tres roles del sistema.
 * El borrado es lógico (active = false), nunca físico.
 */
public final class User {

    public final String id;
    public final String name;
    public final String email;
    public final String phone;
    public final String password; // solo para el mock; nunca se persiste ni se expone en UI
    public final Role role;
    public final boolean active;  // borrado lógico / bloqueo de cuenta

    public User(String id, String name, String email, String phone, String password, Role role) {
        this(id, name, email, phone, password, role, true);
    }

    public User(String id, String name, String email, String phone, String password,
                Role role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email == null ? null : email.trim().toLowerCase();
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    /** Email normalizado (minúsculas y sin espacios en extremos). */
    @NonNull
    public String normalizedEmail() {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
