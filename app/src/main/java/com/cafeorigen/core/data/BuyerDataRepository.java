package com.cafeorigen.core.data;

import android.os.Handler;
import android.os.Looper;

import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CafeOrigen — Repositorio del Comprador (C1–C6).
 * Catálogo (solo cafés ACTIVOS), checkout que agrupa por vendedor y crea los
 * pedidos (V6 los ve cada vendedor, C6 el comprador), historial y destacados.
 * La pasarela es una interfaz demostrativa: procesa la confirmación en el
 * mock, sin tocar proveedores de pago reales ni datos bancarios.
 */
public final class BuyerDataRepository {

    /** Datos de entrega del checkout (C5). */
    public static final class Delivery {
        public final String recipientName;
        public final String phone;
        public final String address;

        public Delivery(String recipientName, String phone, String address) {
            this.recipientName = recipientName;
            this.phone = phone;
            this.address = address;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long LATENCY_MS = 350L;

    private static volatile BuyerDataRepository instance;

    public static BuyerDataRepository get() {
        if (instance == null) {
            synchronized (BuyerDataRepository.class) {
                if (instance == null) {
                    instance = new BuyerDataRepository();
                }
            }
        }
        return instance;
    }

    private BuyerDataRepository() {
    }

    private <T> void async(final java.util.concurrent.Callable<UserRepository.Result<T>> work,
                           final UserRepository.Callback<T> callback) {
        EXECUTOR.execute(() -> {
            UserRepository.Result<T> result;
            try {
                result = work.call();
            } catch (Exception e) {
                result = UserRepository.Result.failure(e.getMessage() == null
                        ? "mock_error" : e.getMessage());
            }
            final UserRepository.Result<T> finalResult = result;
            MAIN.postDelayed(() -> callback.onResult(finalResult), LATENCY_MS);
        });
    }

    // =================== CATÁLOGO (C2) ===================

    /** Café publicado y aprobado (solo ACTIVO); nunca borradores/moderadas. */
    public void catalog(UserRepository.Callback<List<Product>> callback) {
        async(() -> {
            List<Product> active = new ArrayList<>();
            for (Product product : MockDataProvider.products()) {
                if (product.status == com.cafeorigen.core.model.ProductStatus.ACTIVO
                        && product.stock > 0) {
                    active.add(product);
                }
            }
            return UserRepository.Result.success(active);
        }, callback);
    }

    /** Destacados para el inicio (C1): primeros cafés activos. */
    public void featured(int limit, UserRepository.Callback<List<Product>> callback) {
        async(() -> {
            List<Product> active = new ArrayList<>();
            for (Product product : MockDataProvider.products()) {
                if (product.status == com.cafeorigen.core.model.ProductStatus.ACTIVO
                        && product.stock > 0) {
                    active.add(product);
                }
            }
            List<Product> featured = active.size() > limit
                    ? active.subList(0, limit) : active;
            return UserRepository.Result.success(new ArrayList<>(featured));
        }, callback);
    }

    // =================== CHECKOUT (C5) ===================

    /**
     * Confirma la compra: valida existencias, agrupa las líneas por vendedor
     * (cada finca recibe su pedido propio) y descuenta stock. El carrito se
     * vacía en la UI cuando llega la confirmación.
     * Devuelve los pedidos creados (uno por vendedor) o error "stock_changed".
     */
    public void checkout(final String buyerId, final String buyerName,
                         final Delivery delivery,
                         final List<CartRepository.CartItem> cartItems,
                         UserRepository.Callback<List<Order>> callback) {
        async(() -> {
            if (delivery.recipientName == null || delivery.address == null) {
                return UserRepository.Result.<List<Order>>failure("delivery_invalid");
            }
            // 1. Validar todas las líneas antes de mutar el mock.
            Map<String, Product> snapshot = new HashMap<>();
            for (CartRepository.CartItem item : cartItems) {
                Product product = MockDataProvider.findProductById(item.productId);
                if (product == null
                        || product.status != com.cafeorigen.core.model.ProductStatus.ACTIVO
                        || product.stock < item.units) {
                    return UserRepository.Result.<List<Order>>failure("stock_changed");
                }
                snapshot.put(product.id, product);
            }
            // 2. Agrupar por vendorId → un pedido por finca.
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
            // 3. Crear pedidos y descontar existencias.
            List<Order> created = new ArrayList<>();
            for (Map.Entry<String, List<OrderItem>> entry : byVendor.entrySet()) {
                Order order = MockDataProvider.addOrder(new Order(
                        "O-" + nextOrderId(), buyerId, entry.getKey(),
                        buyerName, delivery.address, 0, OrderStatus.POR_CONFIRMAR,
                        entry.getValue()));
                created.add(order);
            }
            for (CartRepository.CartItem item : cartItems) {
                Product product = snapshot.get(item.productId);
                MockDataProvider.decrementStock(product.id, item.units);
            }
            return UserRepository.Result.success(created);
        }, callback);
    }

    private static int orderSeq = 300;

    private static int nextOrderId() {
        orderSeq++;
        return orderSeq;
    }

    // =================== HISTORIAL (C6) ===================

    /** Solo los pedidos del comprador en sesión (permiso buyerId). */
    public void buyerOrders(final String buyerId,
                            UserRepository.Callback<List<Order>> callback) {
        async(() -> UserRepository.Result.success(
                MockDataProvider.ordersForBuyer(buyerId)), callback);
    }
}
