package com.cafeorigen.core.model;

/**
 * CafeOrigen — Roles de la aplicación. El control de acceso (T5/T6) restringe
 * destinos y menús según el rol guardado en la sesión.
 */
public enum Role {
    COMPRADOR("Comprador"),
    VENDEDOR("Vendedor"),
    ADMINISTRADOR("Administrador");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Etiqueta legible en UI (español de Colombia). */
    public String getLabel() {
        return label;
    }

    /** Valor estable para persistir en SharedPreferences. */
    public String asKey() {
        return name();
    }

    /** Reconstruye el rol guardado, con COMPRADOR como mínimo privilegio. */
    public static Role fromKey(String key) {
        if (key == null) {
            return COMPRADOR;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return COMPRADOR;
        }
    }
}
