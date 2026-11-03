package com.cafeorigen.core.model;

/**
 * CafeOrigen — Registro de venta agregado por producto y día
 * (fuente mock para dashboard y reporte de ventas).
 */
public final class SaleRecord {

    public final String productId;
    public final int daysAgo;   // 0 = hoy
    public final int units;     // libras vendidas

    public SaleRecord(String productId, int daysAgo, int units) {
        this.productId = productId;
        this.daysAgo = daysAgo;
        this.units = units;
    }
}
