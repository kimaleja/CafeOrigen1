package com.cafeorigen.core.data;

import com.cafeorigen.core.data.firebase.FirebaseBootstrap;
import com.cafeorigen.core.data.firebase.FirebaseCartDataSource;
import com.cafeorigen.core.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CafeOrigen — Carrito de compras.
 * En modo mock vive solo en memoria (sobrevive a la navegación entre tabs).
 * En modo Firebase se persiste en carts/{uid}: al iniciar sesión se carga del
 * servidor y cada cambio se escribe en segundo plano (write-through).
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

    /** uid del carrito persistido (null = solo memoria). */
    private String cloudUid;
    /** Aviso a la UI cuando el carrito cambia desde Firestore. */
    private Runnable onChangedListener;

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

    // ==================== PERSISTENCIA (Firestore) ====================

    /**
     * Asocia el carrito al usuario autenticado y trae su contenido guardado.
     * Sin Firebase, el carrito sigue siendo local y no hace nada.
     */
    public synchronized void attach(String uid) {
        if (!FirebaseBootstrap.isConfigured() || uid == null) {
            cloudUid = null;
            return;
        }
        cloudUid = uid;
        FirebaseCartDataSource.get().load(uid, result -> {
            if (!result.isSuccess()) {
                return; // se conserva el carrito en memoria y se reintenta al cambiar algo
            }
            synchronized (CartRepository.this) {
                ITEMS.clear();
                if (result.data != null) {
                    ITEMS.addAll(result.data);
                }
            }
            notifyChanged();
        });
    }

    /** Desasocia y limpia el carrito (cierre de sesión / cambio de usuario). */
    public synchronized void detach() {
        cloudUid = null;
        ITEMS.clear();
        FirebaseCartDataSource.get().reset();
        notifyChanged();
    }

    public void setOnChangedListener(Runnable listener) {
        this.onChangedListener = listener;
    }

    private void notifyChanged() {
        Runnable listener = onChangedListener;
        if (listener != null) {
            listener.run();
        }
    }

    /** Escribe el estado actual (agrupando ráfagas de cambios). */
    private void persist() {
        if (cloudUid != null && FirebaseBootstrap.isConfigured()) {
            FirebaseCartDataSource.get().save(cloudUid, new ArrayList<>(ITEMS));
        }
    }

    /** Vacía sin intentar escribir de nuevo (tras el checkout transaccional). */
    public synchronized void clearLocal() {
        ITEMS.clear();
        notifyChanged();
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
        } else {
            ITEMS.add(new CartItem(productId, target));
        }
        persist();
        notifyChanged();
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
                persist();
                notifyChanged();
                return true;
            }
        }
        return false;
    }

    /** Quita una línea del carrito. */
    public synchronized boolean remove(String productId) {
        boolean removed = ITEMS.removeIf(item -> item.productId.equals(productId));
        if (removed) {
            persist();
            notifyChanged();
        }
        return removed;
    }

    /** Vacía el carrito (tras confirmar la compra). */
    public synchronized void clear() {
        ITEMS.clear();
        persist();
        notifyChanged();
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
