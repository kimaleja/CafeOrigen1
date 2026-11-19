package com.cafeorigen.core.data.firebase;

import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CafeOrigen — Espejo en memoria de los documentos leídos de Firestore.
 *
 * La UI existente consulta algunos datos de forma síncrona
 * (MockDataProvider.findProductById, findById, findOrderById) para pintar
 * detalles y carrito. En modo Firebase esas consultas se resuelven desde este
 * espejo, que se llena con cada lectura real (catálogo, listas, perfiles).
 *
 * No sustituye a Firestore: es una caché de proceso. Si un documento no se
 * leyó todavía se devuelve null y la pantalla aplica su estado vacío.
 */
public final class FirestoreCache {

    private static final Map<String, Product> PRODUCTS = new ConcurrentHashMap<>();
    private static final Map<String, Order> ORDERS = new ConcurrentHashMap<>();
    private static final Map<String, User> USERS = new ConcurrentHashMap<>();

    private FirestoreCache() {
    }

    public static void put(Product product) {
        if (product != null && product.id != null) {
            PRODUCTS.put(product.id, product);
        }
    }

    public static Product product(String id) {
        return id == null ? null : PRODUCTS.get(id);
    }

    public static void put(Order order) {
        if (order != null && order.id != null) {
            ORDERS.put(order.id, order);
        }
    }

    public static Order order(String id) {
        return id == null ? null : ORDERS.get(id);
    }

    public static void put(User user) {
        if (user != null && user.id != null) {
            USERS.put(user.id, user);
        }
    }

    public static User user(String id) {
        return id == null ? null : USERS.get(id);
    }

    public static void removeProduct(String id) {
        if (id != null) {
            PRODUCTS.remove(id);
        }
    }

    public static void removeOrder(String id) {
        if (id != null) {
            ORDERS.remove(id);
        }
    }

    /** Limpia el espejo al cerrar sesión para no mezclar datos entre cuentas. */
    public static void clear() {
        PRODUCTS.clear();
        ORDERS.clear();
        USERS.clear();
    }
}
