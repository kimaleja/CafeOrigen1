package com.cafeorigen.core.data.firebase;

import com.cafeorigen.core.data.BuyerDataRepository;
import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CafeOrigen — Pedidos en Cloud Firestore.
 *
 * El checkout es una transacción: valida existencias, descuenta stock y crea
 * un pedido por vendedor en la misma operación atómica. Si dos compradores
 * pelean por la última libra, Firestore reintenta y solo uno gana.
 */
public final class FirebaseOrderDataSource {

    private static final FirebaseOrderDataSource INSTANCE = new FirebaseOrderDataSource();

    public static FirebaseOrderDataSource get() {
        return INSTANCE;
    }

    private FirebaseOrderDataSource() {
    }

    // ==================== CHECKOUT ====================

    public void checkout(final String buyerId, final String buyerName,
                         final BuyerDataRepository.Delivery delivery,
                         final List<CartRepository.CartItem> cartItems,
                         final UserRepository.Callback<List<Order>> callback) {
        if (delivery == null || delivery.recipientName == null || delivery.address == null) {
            callback.onResult(UserRepository.Result.<List<Order>>failure("delivery_invalid"));
            return;
        }
        if (cartItems == null || cartItems.isEmpty()) {
            callback.onResult(UserRepository.Result.<List<Order>>failure("empty_cart"));
            return;
        }
        final String key = "order:checkout:" + buyerId;
        if (!InFlightGuard.begin(key)) {
            callback.onResult(UserRepository.Result.<List<Order>>failure(
                    FirebaseErrorMapper.IN_PROGRESS));
            return;
        }

        final FirebaseFirestore db = FirebaseBootstrap.db();
        db.runTransaction(transaction -> {
            // 1) Lecturas: todas antes de cualquier escritura (requisito de Firestore).
            Map<String, Product> snapshot = new HashMap<>();
            for (CartRepository.CartItem item : cartItems) {
                DocumentSnapshot doc = transaction.get(
                        db.collection(FirestoreSchema.PRODUCTS).document(item.productId));
                Product product = FirebaseMappers.toProduct(doc);
                if (product == null || product.status != ProductStatus.ACTIVO
                        || product.stock < item.units) {
                    throw new StockChangedException();
                }
                snapshot.put(product.id, product);
            }

            // 2) Agrupar por vendedor: un pedido por finca.
            Map<String, List<OrderItem>> byVendor = new HashMap<>();
            for (CartRepository.CartItem item : cartItems) {
                Product product = snapshot.get(item.productId);
                List<OrderItem> lines = byVendor.get(product.vendorId);
                if (lines == null) {
                    lines = new ArrayList<>();
                    byVendor.put(product.vendorId, lines);
                }
                lines.add(new OrderItem(product.id, product.name, item.units,
                        (long) product.price * item.units));
            }

            // 3) Escrituras: pedidos + descuento de stock + carrito vacío.
            List<Order> created = new ArrayList<>();
            Timestamp now = Timestamp.now();
            for (Map.Entry<String, List<OrderItem>> entry : byVendor.entrySet()) {
                DocumentReference ref = db.collection(FirestoreSchema.ORDERS).document();
                Order order = new Order(ref.getId(), buyerId, entry.getKey(), buyerName,
                        delivery.address, 0, OrderStatus.POR_CONFIRMAR, entry.getValue());
                transaction.set(ref, FirebaseMappers.orderToMap(order, now));
                created.add(order);
            }
            for (CartRepository.CartItem item : cartItems) {
                Product product = snapshot.get(item.productId);
                transaction.update(db.collection(FirestoreSchema.PRODUCTS)
                                .document(item.productId),
                        FirestoreSchema.STOCK, product.stock - item.units);
            }
            transaction.delete(db.collection(FirestoreSchema.CARTS).document(buyerId));
            return created;
        }).addOnCompleteListener(task -> {
            InFlightGuard.end(key);
            if (!task.isSuccessful()) {
                String code = task.getException() instanceof StockChangedException
                        ? "stock_changed"
                        : FirebaseErrorMapper.map(task.getException());
                callback.onResult(UserRepository.Result.<List<Order>>failure(code));
                return;
            }
            List<Order> created = task.getResult() == null
                    ? new ArrayList<>() : task.getResult();
            for (Order order : created) {
                FirestoreCache.put(order);
            }
            callback.onResult(UserRepository.Result.success(created));
        });
    }

    // ==================== CONSULTAS ====================

    public void ordersForBuyer(final String buyerId,
                               final UserRepository.Callback<List<Order>> callback) {
        queryAndDeliver(FirestoreSchema.BUYER_ID, buyerId, callback);
    }

    public void ordersForVendor(final String vendorId,
                                final UserRepository.Callback<List<Order>> callback) {
        queryAndDeliver(FirestoreSchema.VENDOR_ID, vendorId, callback);
    }

    /** Todos los pedidos: solo el administrador (las reglas lo exigen). */
    public void allOrders(final UserRepository.Callback<List<Order>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.ORDERS).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    callback.onResult(UserRepository.Result.success(
                            toOrders(task.getResult().getDocuments())));
                });
    }

    private void queryAndDeliver(String field, String value,
                                 final UserRepository.Callback<List<Order>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.ORDERS)
                .whereEqualTo(field, value)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    callback.onResult(UserRepository.Result.success(
                            toOrders(task.getResult().getDocuments())));
                });
    }

    /** Convierte, cachea y ordena por fecha descendente (más reciente primero). */
    private List<Order> toOrders(List<DocumentSnapshot> documents) {
        List<Order> orders = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            Order order = FirebaseMappers.toOrder(doc);
            if (order != null) {
                FirestoreCache.put(order);
                orders.add(order);
            }
        }
        Collections.sort(orders, (a, b) -> Integer.compare(a.daysAgo, b.daysAgo));
        return orders;
    }

    // ==================== CAMBIO DE ESTADO ====================

    /**
     * El vendedor dueño avanza el estado del pedido. El comprador no puede
     * modificarlo: su repositorio no expone esta operación y las reglas
     * tampoco lo permiten.
     */
    public void updateStatus(final String orderId, final OrderStatus newStatus,
                             final UserRepository.Callback<Order> callback) {
        final String key = "order:status:" + orderId;
        if (!InFlightGuard.begin(key)) {
            callback.onResult(UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        final String uid = FirebaseBootstrap.currentUid();
        final DocumentReference ref = FirebaseBootstrap.db()
                .collection(FirestoreSchema.ORDERS).document(orderId);
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<Order>failure(
                        FirebaseErrorMapper.map(task.getException())));
                return;
            }
            Order order = FirebaseMappers.toOrder(task.getResult());
            if (order == null) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<Order>failure("not_found"));
                return;
            }
            if (uid == null || !uid.equals(order.vendorId)) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<Order>failure(
                        FirebaseErrorMapper.PERMISSION));
                return;
            }
            Map<String, Object> change = new HashMap<>();
            change.put(FirestoreSchema.STATUS, newStatus.name());
            change.put(FirestoreSchema.UPDATED_AT, com.google.firebase.firestore.FieldValue
                    .serverTimestamp());
            ref.update(change).addOnCompleteListener(write -> {
                InFlightGuard.end(key);
                if (!write.isSuccessful()) {
                    callback.onResult(UserRepository.Result.<Order>failure(
                            FirebaseErrorMapper.map(write.getException())));
                    return;
                }
                Order updated = order.withStatus(newStatus);
                FirestoreCache.put(updated);
                callback.onResult(UserRepository.Result.success(updated));
            });
        });
    }

    /** Excepción propia para distinguir el conflicto de stock de un fallo técnico. */
    private static final class StockChangedException extends RuntimeException {
        StockChangedException() {
            super("stock_changed");
        }
    }
}
