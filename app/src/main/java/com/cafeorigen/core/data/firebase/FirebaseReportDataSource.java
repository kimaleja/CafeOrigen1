package com.cafeorigen.core.data.firebase;

import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.data.VendorDataRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CafeOrigen — Agregados de dashboard y reporte calculados sobre pedidos
 * reales de Firestore.
 *
 * Diferencia consciente con el mock: las cifras ya no salen de una tabla
 * "sales" sintética, sino de los pedidos confirmados (se excluyen CANCELADO).
 * Un pedido cuenta en el día en que se creó (createdAt).
 */
public final class FirebaseReportDataSource {

    private static final int ADMIN_LOW_STOCK_MAX = 20;
    private static final int VENDOR_LOW_STOCK_MAX = 15;

    private static final FirebaseReportDataSource INSTANCE = new FirebaseReportDataSource();

    public static FirebaseReportDataSource get() {
        return INSTANCE;
    }

    private FirebaseReportDataSource() {
    }

    // ==================== DASHBOARD ADMIN ====================

    public void adminDashboard(final UserRepository.Callback<AdminDataRepository.DashboardStats> cb) {
        final com.google.firebase.firestore.FirebaseFirestore db = FirebaseBootstrap.db();
        db.collection(FirestoreSchema.USERS).get().addOnCompleteListener(usersTask -> {
            if (!usersTask.isSuccessful()) {
                cb.onResult(UserRepository.Result.failure(
                        FirebaseErrorMapper.map(usersTask.getException())));
                return;
            }
            final int usersCount = sizeOf(usersTask.getResult());
            db.collection(FirestoreSchema.PRODUCTS).get().addOnCompleteListener(productsTask -> {
                if (!productsTask.isSuccessful()) {
                    cb.onResult(UserRepository.Result.failure(
                            FirebaseErrorMapper.map(productsTask.getException())));
                    return;
                }
                final List<Product> products = products(productsTask.getResult());
                db.collection(FirestoreSchema.ORDERS).get().addOnCompleteListener(ordersTask -> {
                    if (!ordersTask.isSuccessful()) {
                        cb.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(ordersTask.getException())));
                        return;
                    }
                    List<Order> orders = orders(ordersTask.getResult());
                    long revenueMonth = 0;
                    float[] weekly = new float[7];
                    for (Order order : orders) {
                        if (order.status == OrderStatus.CANCELADO) {
                            continue;
                        }
                        if (order.daysAgo < 30) {
                            revenueMonth += order.total;
                        }
                        if (order.daysAgo >= 0 && order.daysAgo < 7) {
                            weekly[6 - order.daysAgo] += order.total;
                        }
                    }

                    List<Product> pending = new ArrayList<>();
                    List<Product> lowStock = new ArrayList<>();
                    for (Product product : products) {
                        if (product.status == ProductStatus.PENDIENTE) {
                            pending.add(product);
                        } else if (product.status == ProductStatus.ACTIVO
                                && product.stock < ADMIN_LOW_STOCK_MAX) {
                            lowStock.add(product);
                        }
                    }
                    cb.onResult(UserRepository.Result.success(
                            new AdminDataRepository.DashboardStats(revenueMonth, usersCount,
                                    products.size(), pending.size(), lowStock, pending,
                                    weekly)));
                });
            });
        });
    }

    // ==================== DASHBOARD VENDEDOR ====================

    public void vendorDashboard(final String vendorId,
                                final UserRepository.Callback<VendorDataRepository.VendorDashboard> cb) {
        final com.google.firebase.firestore.FirebaseFirestore db = FirebaseBootstrap.db();
        db.collection(FirestoreSchema.PRODUCTS)
                .whereEqualTo(FirestoreSchema.VENDOR_ID, vendorId)
                .get()
                .addOnCompleteListener(productsTask -> {
                    if (!productsTask.isSuccessful()) {
                        cb.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(productsTask.getException())));
                        return;
                    }
                    final List<Product> products = products(productsTask.getResult());
                    db.collection(FirestoreSchema.ORDERS)
                            .whereEqualTo(FirestoreSchema.VENDOR_ID, vendorId)
                            .get()
                            .addOnCompleteListener(ordersTask -> {
                                if (!ordersTask.isSuccessful()) {
                                    cb.onResult(UserRepository.Result.failure(
                                            FirebaseErrorMapper.map(ordersTask.getException())));
                                    return;
                                }
                                List<Order> orders = orders(ordersTask.getResult());

                                long salesMonth = 0;
                                float[] weekly = new float[7];
                                int pendingCount = 0;
                                List<Order> toConfirm = new ArrayList<>();
                                for (Order order : orders) {
                                    if (order.status == OrderStatus.POR_CONFIRMAR) {
                                        toConfirm.add(order);
                                        pendingCount++;
                                    }
                                    if (order.status == OrderStatus.CANCELADO) {
                                        continue;
                                    }
                                    if (order.daysAgo < 30) {
                                        salesMonth += order.total;
                                    }
                                    if (order.daysAgo >= 0 && order.daysAgo < 7) {
                                        weekly[6 - order.daysAgo] += order.total;
                                    }
                                }

                                int activeProducts = 0;
                                List<Product> lowStock = new ArrayList<>();
                                for (Product product : products) {
                                    if (product.status == ProductStatus.ACTIVO) {
                                        activeProducts++;
                                        if (product.stock < VENDOR_LOW_STOCK_MAX) {
                                            lowStock.add(product);
                                        }
                                    }
                                }
                                cb.onResult(UserRepository.Result.success(
                                        new VendorDataRepository.VendorDashboard(salesMonth,
                                                activeProducts, pendingCount, lowStock,
                                                toConfirm, weekly)));
                            });
                });
    }

    // ==================== REPORTE (A7) ====================

    public void buildReport(final int days,
                            final UserRepository.Callback<AdminDataRepository.ReportData> cb) {
        final int period = Math.max(1, days);
        final com.google.firebase.firestore.FirebaseFirestore db = FirebaseBootstrap.db();
        db.collection(FirestoreSchema.PRODUCTS).get().addOnCompleteListener(productsTask -> {
            if (!productsTask.isSuccessful()) {
                cb.onResult(UserRepository.Result.failure(
                        FirebaseErrorMapper.map(productsTask.getException())));
                return;
            }
            final Map<String, Product> productsById = new HashMap<>();
            for (Product product : products(productsTask.getResult())) {
                productsById.put(product.id, product);
            }
            db.collection(FirestoreSchema.ORDERS).get().addOnCompleteListener(ordersTask -> {
                if (!ordersTask.isSuccessful()) {
                    cb.onResult(UserRepository.Result.failure(
                            FirebaseErrorMapper.map(ordersTask.getException())));
                    return;
                }
                Map<String, long[]> byProduct = new HashMap<>(); // [0]=unidades [1]=ingresos
                Map<String, String[]> labels = new HashMap<>();  // [0]=nombre [1]=región
                float[] daily = new float[period];
                long revenue = 0;
                int units = 0;

                for (Order order : orders(ordersTask.getResult())) {
                    if (order.status == OrderStatus.CANCELADO || order.daysAgo >= period) {
                        continue;
                    }
                    for (OrderItem item : order.items) {
                        long[] aggregate = byProduct.get(item.productId);
                        if (aggregate == null) {
                            aggregate = new long[2];
                            byProduct.put(item.productId, aggregate);
                        }
                        aggregate[0] += item.units;
                        aggregate[1] += item.subtotal;
                        revenue += item.subtotal;
                        units += item.units;

                        if (!labels.containsKey(item.productId)) {
                            Product product = productsById.get(item.productId);
                            labels.put(item.productId, new String[]{
                                    item.productName,
                                    product == null ? "" : product.region});
                        }
                    }
                    daily[period - 1 - order.daysAgo] += order.total;
                }

                List<AdminDataRepository.ReportRow> rows = new ArrayList<>();
                for (Map.Entry<String, long[]> entry : byProduct.entrySet()) {
                    String[] label = labels.get(entry.getKey());
                    rows.add(new AdminDataRepository.ReportRow(
                            label == null ? entry.getKey() : label[0],
                            label == null ? "" : label[1],
                            (int) entry.getValue()[0],
                            entry.getValue()[1]));
                }
                rows.sort((a, b) -> Long.compare(b.revenue, a.revenue));
                cb.onResult(UserRepository.Result.success(
                        new AdminDataRepository.ReportData(period, rows, revenue, units, daily)));
            });
        });
    }

    // ==================== AUXILIARES ====================

    private static int sizeOf(QuerySnapshot snapshot) {
        return snapshot == null ? 0 : snapshot.size();
    }

    private static List<Product> products(QuerySnapshot snapshot) {
        List<Product> list = new ArrayList<>();
        if (snapshot == null) {
            return list;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Product product = FirebaseMappers.toProduct(doc);
            if (product != null) {
                FirestoreCache.put(product);
                list.add(product);
            }
        }
        return list;
    }

    private static List<Order> orders(QuerySnapshot snapshot) {
        List<Order> list = new ArrayList<>();
        if (snapshot == null) {
            return list;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Order order = FirebaseMappers.toOrder(doc);
            if (order != null) {
                FirestoreCache.put(order);
                list.add(order);
            }
        }
        return list;
    }
}
