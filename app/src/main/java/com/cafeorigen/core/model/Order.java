package com.cafeorigen.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — Modelo de pedido (mock local).
 * vendorId define al vendedor dueño; buyerId al comprador que lo realizó:
 * cada rol solo ve los pedidos con su id (permisos en la capa de datos).
 */
public final class Order {

    public final String id;
    public final String buyerId;       // permiso: solo su dueño lo ve (comprador)
    public final String vendorId;      // permiso: solo su dueño lo ve (vendedor)
    public final String buyerName;
    public final String address;
    public final int daysAgo;          // 0 = recibido hoy
    public final OrderStatus status;
    public final List<OrderItem> items;
    public final long total;

    public Order(String id, String buyerId, String vendorId, String buyerName, String address,
                 int daysAgo, OrderStatus status, List<OrderItem> items) {
        this.id = id;
        this.buyerId = buyerId;
        this.vendorId = vendorId;
        this.buyerName = buyerName;
        this.address = address;
        this.daysAgo = daysAgo;
        this.status = status;
        this.items = items == null ? new ArrayList<>() : items;
        long sum = 0;
        for (OrderItem item : this.items) {
            sum += item.subtotal;
        }
        this.total = sum;
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, buyerId, vendorId, buyerName, address, daysAgo, newStatus, items);
    }
}
