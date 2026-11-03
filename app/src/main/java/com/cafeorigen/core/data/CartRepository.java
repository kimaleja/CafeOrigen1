package com.cafeorigen.core.data;

import com.cafeorigen.core.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CafeOrigen — Carrito de compras en memoria (sin backend).
 * Sobrevive a la navegación entre tabs y sub-pantallas de la misma sesión.
 * Valida existencias contra el mock al agregar/actualizar unidades.
 */
public final class CartRepository {

    /** Línea de carrito inmutable (producto + unidades). */
    public static final class CartItem {
        public final String productId;
        public final int units;

        public CartItem(String productId, int units) {
            this.productId = productId;
            this.units = units;
        }
    }

    private static final List<CartItem> ITEMS = new CopyOnWriteArrayList<>();

    private static volatile CartRepository instance;

    public static CartRepository get() {
        if (instance == null) {
            synchronized (CartRepository.class) {
                if (instance == null) {
                    instance = new CartRepository();
                }
            }
        }
        return instance;
    }

    private CartRepository() {
    }

    /** Agrega (o suma) unidades; respeta el stock disponible. */
    public synchronized boolean add(String productId, int units) {
        Product product = MockDataProvider.findProductById(productId);
        if (product == null || units < 1) {
            return false;
        }
        int existing = 0;
        for (CartItem item : ITEMS) {
            if (item.productId.equals(productId)) {
                existing = item.units;
                break;
            }
        }
        int target = existing + units;
        if (target > product.stock) {
            return false;
        }
        if (existing > 0) {
            replace(productId, new CartItem(productId, target));
            return true;
        }
        ITEMS.add(new CartItem(productId, target));
        return true;
    }

    /** Cambia las unidades de una línea; 0 lo elimina. Respeta stock. */
    public synchronized boolean updateUnits(String productId, int units) {
        if (units < 0) {
            return false;
        }
        if (units == 0) {
            return remove(productId);
        }
        Product product = MockDataProvider.findProductById(productId);
        if (product == null || units > product.stock) {
            return false;
        }
        for (CartItem item : ITEMS) {
            if (item.productId.equals(productId)) {
                replace(productId, new CartItem(productId, units));
                return true;
            }
        }
        return false;
    }

    /** Quita una línea del carrito. */
    public synchronized boolean remove(String productId) {
        return ITEMS.removeIf(item -> item.productId.equals(productId));
    }

    /** Vacía el carrito (tras confirmar la compra). */
    public synchronized void clear() {
        ITEMS.clear();
    }

    /** Snapshot de líneas (solo lectura). */
    public synchronized List<CartItem> items() {
        return new ArrayList<>(ITEMS);
    }

    /** Número de líneas. */
    public synchronized int count() {
        return ITEMS.size();
    }

    /** Total del carrito con precios actuales (0 si vacío). */
    public synchronized long total() {
        long total = 0;
        for (CartItem item : ITEMS) {
            Product product = MockDataProvider.findProductById(item.productId);
            if (product != null) {
                total += (long) product.price * item.units;
            }
        }
        return total;
    }

    /** Unidades acumuladas de un producto (para el stepper del detalle). */
    public synchronized int unitsOf(String productId) {
        for (CartItem item : ITEMS) {
            if (item.productId.equals(productId)) {
                return item.units;
            }
        }
        return 0;
    }

    private void replace(String productId, CartItem replacement) {
        for (int index = 0; index < ITEMS.size(); index++) {
            if (ITEMS.get(index).productId.equals(productId)) {
                ITEMS.set(index, replacement);
                return;
            }
        }
        ITEMS.add(replacement);
    }
}
