package com.cafeorigen.core.model;

/**
 * CafeOrigen — Estados del ciclo de un pedido desde la vista del Vendedor.
 * Transiciones: POR_CONFIRMAR → CONFIRMADO → DESPACHADO → ENTREGADO
 * (o CANCELADO desde POR_CONFIRMAR).
 */
public enum OrderStatus {
    POR_CONFIRMAR("Por confirmar"),
    CONFIRMADO("Confirmado"),
    DESPACHADO("Despachado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
