package com.cafeorigen.core.model;

/**
 * CafeOrigen — Línea de un pedido (producto + cantidad).
 * Insuma para el detalle de pedido del Vendedor y el historial del Comprador.
 */
public final class OrderItem {

    public final String productId;
    public final String productName;
    public final int units;      // libras
    public final long subtotal;  // COP

    public OrderItem(String productId, String productName, int units, long subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.units = units;
        this.subtotal = subtotal;
    }
}
