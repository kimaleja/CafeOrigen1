package com.cafeorigen.core.data;

import android.os.Handler;
import android.os.Looper;

import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.SaleRecord;
import com.cafeorigen.core.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CafeOrigen — Repositorio del Vendedor (V1–V7).
 * Todas las consultas y mutaciones se filtran por vendorId: el vendedor solo
 * puede ver/editar sus productos y sus pedidos recibidos. Se apoya en el mock
 * local sin mezclar lógica de negocio en Activities/Fragments.
 */
public final class VendorDataRepository {

    /** Resumen del panel principal (V1). */
    public static final class VendorDashboard {
        public final long salesMonth;        // ingresos 30 días
        public final int activeProducts;
        public final int pendingOrders;      // POR_CONFIRMAR
        public final int lowStockCount;      // ACTIVO con stock < 15 lb
        public final List<Product> lowStock;
        public final List<Order> toConfirm;  // pedidos por confirmar
        public final float[] weeklyChart;    // ingresos últimos 7 días

        VendorDashboard(long salesMonth, int activeProducts, int pendingOrders,
                        List<Product> lowStock, List<Order> toConfirm, float[] weeklyChart) {
            this.salesMonth = salesMonth;
            this.activeProducts = activeProducts;
            this.pendingOrders = pendingOrders;
            this.lowStockCount = lowStock.size();
            this.lowStock = lowStock;
            this.toConfirm = toConfirm;
            this.weeklyChart = weeklyChart;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long LATENCY_MS = 350L;
    private static final int LOW_STOCK_MAX = 15;

    private static volatile VendorDataRepository instance;

    public static VendorDataRepository get() {
        if (instance == null) {
            synchronized (VendorDataRepository.class) {
                if (instance == null) {
                    instance = new VendorDataRepository();
                }
            }
        }
        return instance;
    }

    private VendorDataRepository() {
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

    // =================== PRODUCTOS (V2–V5) ===================

    /** Solo los productos del vendedor dueño (permisos por vendorId). */
    public void listProducts(final String vendorId,
                             UserRepository.Callback<List<Product>> callback) {
        async(() -> UserRepository.Result.success(MockDataProvider.productsFor(vendorId)), callback);
    }

    /** Crea (V3) o actualiza (V4) un producto del vendedor. */
    public void saveProduct(final Product draft, final boolean isNew,
                            UserRepository.Callback<Product> callback) {
        async(() -> {
            if (isNew) {
                return UserRepository.Result.success(MockDataProvider.addProduct(draft));
            }
            boolean ok = MockDataProvider.updateProduct(draft);
            Product updated = ok ? MockDataProvider.findProductById(draft.id) : null;
            return ok ? UserRepository.Result.success(updated)
                    : UserRepository.Result.<Product>failure("not_found");
        }, callback);
    }

    /** Borrado de producto (V5); el mock lo retira de la lista. */
    public void deleteProduct(final String productId, UserRepository.Callback<Boolean> callback) {
        async(() -> UserRepository.Result.success(MockDataProvider.removeProduct(productId)), callback);
    }

    // =================== PEDIDOS (V6–V7) ===================

    /** Solo los pedidos recibidos por este vendedor. */
    public void listOrders(final String vendorId, UserRepository.Callback<List<Order>> callback) {
        async(() -> UserRepository.Result.success(MockDataProvider.ordersFor(vendorId)), callback);
    }

    /** Cambia el estado de un pedido propio (historial V6 + detalle V7). */
    public void updateOrderStatus(final String orderId, final OrderStatus newStatus,
                                  UserRepository.Callback<Order> callback) {
        async(() -> {
            boolean ok = MockDataProvider.updateOrderStatus(orderId, newStatus);
            Order order = ok ? MockDataProvider.findOrderById(orderId) : null;
            return ok ? UserRepository.Result.success(order)
                    : UserRepository.Result.<Order>failure("not_found");
        }, callback);
    }

    // =================== DASHBOARD (V1) ===================

    public void dashboardFor(final String vendorId,
                             UserRepository.Callback<VendorDashboard> callback) {
        async(() -> {
            List<Product> ownProducts = MockDataProvider.productsFor(vendorId);
            List<Order> ownOrders = MockDataProvider.ordersFor(vendorId);
            List<SaleRecord> sales = MockDataProvider.sales();

            long salesMonth = 0;
            float[] weeklyChart = new float[7];
            for (SaleRecord sale : sales) {
                Product product = MockDataProvider.findProductById(sale.productId);
                if (product == null || !product.vendorId.equals(vendorId)) {
                    continue;
                }
                long amount = (long) product.price * sale.units;
                if (sale.daysAgo < 30) {
                    salesMonth += amount;
                }
                if (sale.daysAgo < 7 && sale.daysAgo >= 0) {
                    weeklyChart[6 - sale.daysAgo] += amount;
                }
            }

            int activeProducts = 0;
            List<Product> lowStock = new ArrayList<>();
            for (Product product : ownProducts) {
                if (product.status == com.cafeorigen.core.model.ProductStatus.ACTIVO) {
                    activeProducts++;
                    if (product.stock < LOW_STOCK_MAX) {
                        lowStock.add(product);
                    }
                }
            }

            int pendingOrders = 0;
            List<Order> toConfirm = new ArrayList<>();
            for (Order order : ownOrders) {
                if (order.status == OrderStatus.POR_CONFIRMAR) {
                    toConfirm.add(order);
                    pendingOrders++;
                }
            }

            return UserRepository.Result.success(new VendorDashboard(
                    salesMonth, activeProducts, pendingOrders, lowStock, toConfirm, weeklyChart));
        }, callback);
    }

    // =================== PERFIL (V8) ===================

    /** Actualiza nombre y teléfono del vendedor (V8). */
    public void updateProfile(final String userId, final String name, final String phone,
                              UserRepository.Callback<User> callback) {
        async(() -> {
            User current = MockDataProvider.findById(userId);
            if (current == null) {
                return UserRepository.Result.<User>failure("not_found");
            }
            User updated = new User(current.id, name, current.email, phone,
                    current.password, current.role, current.active);
            MockDataProvider.updateUser(updated);
            return UserRepository.Result.success(updated);
        }, callback);
    }
}
