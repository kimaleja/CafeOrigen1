package com.cafeorigen.core.model;

/**
 * CafeOrigen — Estados del flujo de publicación de un café:
 * BORRADOR → PENDIENTE (moderación) → ACTIVO | RECHAZADO.
 */
public enum ProductStatus {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente"),
    ACTIVO("Activo"),
    RECHAZADO("Rechazado");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
