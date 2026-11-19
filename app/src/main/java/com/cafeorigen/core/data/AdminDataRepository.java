package com.cafeorigen.core.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.cafeorigen.core.data.firebase.FirebaseBootstrap;
import com.cafeorigen.core.data.firebase.FirebaseProductDataSource;
import com.cafeorigen.core.data.firebase.FirebaseReportDataSource;
import com.cafeorigen.core.data.firebase.FirebaseUserDataSource;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.SaleRecord;
import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CafeOrigen — Repositorio de administración (rol ADMINISTRADOR).
 * Gestión de usuarios (A2–A5), moderación de productos (A6), datos del
 * dashboard (A1) y reporte de ventas (A7). Se apoya en el mock local y
 * entrega los resultados en el hilo principal (LiveData/UI).
 */
public final class AdminDataRepository {

    // ================== Contratos de datos del dashboard y reporte ==================

    /** Agregado de ventas por producto para el reporte (A7). */
    public static final class ReportRow {
        public final String productName;
        public final String region;
        public final int units;
        public final long revenue;

        public ReportRow(String productName, String region, int units, long revenue) {
            this.productName = productName;
            this.region = region;
            this.units = units;
            this.revenue = revenue;
        }
    }

    /** Resultado del reporte de ventas por periodo. */
    public static final class ReportData {
        public final int days;
        public final List<ReportRow> rows;
        public final long totalRevenue;
        public final int totalUnits;
        public final float[] dailyChart; // ingresos por día del periodo (día -N a hoy)

        public ReportData(int days, List<ReportRow> rows, long revenue, int units,
                          float[] chart) {
            this.days = days;
            this.rows = rows;
            this.totalRevenue = revenue;
            this.totalUnits = units;
            this.dailyChart = chart;
        }
    }

    /** Resumen del dashboard (A1). */
    public static final class DashboardStats {
        public final long revenueMonth;
        public final int usersCount;
        public final int inactiveUsers;
        public final int productsCount;
        public final int pendingModeration;
        public final List<Product> lowStock;
        public final List<Product> pendingList;
        public final float[] weeklyChart; // índice 0 = día más antiguo de la semana

        public DashboardStats(long revenue, int users, int inactiveUsers, int products, int pending,
                              List<Product> lowStock, List<Product> pendingList,
                              float[] weeklyChart) {
            this.revenueMonth = revenue;
            this.usersCount = users;
            this.inactiveUsers = inactiveUsers;
            this.productsCount = products;
            this.pendingModeration = pending;
            this.lowStock = lowStock;
            this.pendingList = pendingList;
            this.weeklyChart = weeklyChart;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long LATENCY_MS = 350L;
    private static final int LOW_STOCK_MAX = 20;

    private static volatile AdminDataRepository instance;

    public static AdminDataRepository get() {
        if (instance == null) {
            synchronized (AdminDataRepository.class) {
                if (instance == null) {
                    instance = new AdminDataRepository();
                }
            }
        }
        return instance;
    }

    private AdminDataRepository() {
    }

    /** Ejecuta un cálculo del mock en segundo plano y publica en el principal. */
    private <T> void async(final java.util.concurrent.Callable<UserRepository.Result<T>> work,
                           final UserRepository.Callback<T> callback) {
        EXECUTOR.execute(() -> {
            UserRepository.Result<T> result;
            try {
                result = work.call();
            } catch (Exception e) {
                result = UserRepository.Result.failure(e.getMessage() == null ? "mock_error" : e.getMessage());
            }
            final UserRepository.Result<T> finalResult = result;
            MAIN.postDelayed(() -> callback.onResult(finalResult), LATENCY_MS);
        });
    }

    // =================== USUARIOS (A2–A5) ===================

    public void listUsers(UserRepository.Callback<List<User>> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseUserDataSource.get().listUsers(callback);
            return;
        }
        async(() -> UserRepository.Result.success(MockDataProvider.users()), callback);
    }

    /** Lee una cuenta concreta para editarla sin acceder al mock desde la UI. */
    public void getUser(final String userId, UserRepository.Callback<User> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseUserDataSource.get().fetchProfile(userId, callback);
            return;
        }
        async(() -> {
            User user = MockDataProvider.findById(userId);
            return user == null
                    ? UserRepository.Result.<User>failure("not_found")
                    : UserRepository.Result.success(user);
        }, callback);
    }

    /** Crea usuario con rol (A3) o actualiza existente (A4). */
    public void saveUser(final User draft, final boolean isNew,
                         UserRepository.Callback<User> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            if (!Validators.isValidEmail(draft.email)) {
                callback.onResult(UserRepository.Result.<User>failure("invalid_email"));
                return;
            }
            FirebaseUserDataSource.get().saveUser(draft, isNew, callback);
            return;
        }
        async(() -> {
            if (!Validators.isValidEmail(draft.email)) {
                return UserRepository.Result.<User>failure("invalid_email");
            }
            if (isNew) {
                if (MockDataProvider.findByEmail(draft.email) != null) {
                    return UserRepository.Result.failure("existing_email");
                }
                return UserRepository.Result.success(
                        MockDataProvider.addUserWithRole(draft.name, draft.email, draft.phone,
                                draft.password, draft.role));
            }
            // Edición: el correo puede repetirse solo si es del mismo usuario.
            if (MockDataProvider.findByEmailExcluding(draft.email, draft.id)) {
                return UserRepository.Result.failure("existing_email");
            }
            User current = MockDataProvider.findById(draft.id);
            if (current == null) {
                return UserRepository.Result.failure("not_found");
            }
            // En edición la contraseña queda sin cambios si el campo vino vacío.
            String password = draft.password == null || draft.password.isEmpty()
                    ? current.password : draft.password;
            User updated = new User(current.id, draft.name, draft.email, draft.phone,
                    password, draft.role, draft.active);
            MockDataProvider.updateUser(updated);
            return UserRepository.Result.success(updated);
        }, callback);
    }

    /** Borrado lógico / bloqueo de cuenta (A5). */
    public void setUserActive(final String userId, final boolean active,
                              UserRepository.Callback<User> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseUserDataSource.get().setUserActive(userId, active, callback);
            return;
        }
        async(() -> {
            boolean ok = MockDataProvider.setUserActive(userId, active);
            User user = ok ? MockDataProvider.findById(userId) : null;
            return ok ? UserRepository.Result.success(user)
                      : UserRepository.Result.<User>failure("not_found");
        }, callback);
    }

    // =================== PRODUCTOS (A6) ===================

    public void listProducts(UserRepository.Callback<List<Product>> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseProductDataSource.get().listAll(callback);
            return;
        }
        async(() -> UserRepository.Result.success(MockDataProvider.products()), callback);
    }

    /** Aprueba (ACTIVO) o rechaza (RECHAZADO) un café. */
    public void moderateProduct(final String productId, final ProductStatus newStatus,
                                UserRepository.Callback<Product> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseProductDataSource.get().moderate(productId, newStatus, callback);
            return;
        }
        async(() -> {
            boolean ok = MockDataProvider.moderateProduct(productId, newStatus);
            Product product = ok ? MockDataProvider.findProductById(productId) : null;
            return ok ? UserRepository.Result.success(product)
                      : UserRepository.Result.<Product>failure("not_found");
        }, callback);
    }

    /** Eliminación de producto (mock: retira la entrada; en backend, borrado lógico). */
    public void deleteProduct(final String productId, UserRepository.Callback<Boolean> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseProductDataSource.get().deleteProduct(productId, callback);
            return;
        }
        async(() -> UserRepository.Result.success(MockDataProvider.removeProduct(productId)), callback);
    }

    // =================== DASHBOARD (A1) ===================

    public void dashboardStats(UserRepository.Callback<DashboardStats> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseReportDataSource.get().adminDashboard(callback);
            return;
        }
        async(() -> {
            List<User> users = MockDataProvider.users();
            List<Product> products = MockDataProvider.products();
            List<SaleRecord> sales = MockDataProvider.sales();

            long revenueMonth = 0;
            float[] weeklyChart = new float[7];
            for (SaleRecord sale : sales) {
                Product product = MockDataProvider.findProductById(sale.productId);
                if (product == null) {
                    continue;
                }
                long amount = (long) product.price * sale.units;
                if (sale.daysAgo < 30) {
                    revenueMonth += amount;
                }
                if (sale.daysAgo < 7 && sale.daysAgo >= 0) {
                    weeklyChart[6 - sale.daysAgo] += amount;
                }
            }

            int pending = 0;
            List<Product> pendingList = new ArrayList<>();
            List<Product> lowStock = new ArrayList<>();
            for (Product product : products) {
                if (product.status == ProductStatus.PENDIENTE) {
                    pendingList.add(product);
                    pending++;
                } else if (product.status == ProductStatus.ACTIVO && product.stock < LOW_STOCK_MAX) {
                    lowStock.add(product);
                }
            }

            int inactiveUsers = 0;
            for (User user : users) {
                if (!user.active) {
                    inactiveUsers++;
                }
            }

            DashboardStats stats = new DashboardStats(revenueMonth, users.size(), inactiveUsers,
                    products.size(), pending, lowStock, pendingList, weeklyChart);
            return UserRepository.Result.success(stats);
        }, callback);
    }

    // =================== REPORTE DE VENTAS (A7) ===================

    public void buildReport(final int days, UserRepository.Callback<ReportData> callback) {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseReportDataSource.get().buildReport(days, callback);
            return;
        }
        async(() -> {
            Map<String, int[]> byProduct = new HashMap<>(); // [0]=unidades [1]=ingresos
            float[] daily = new float[days];
            long revenue = 0;
            int units = 0;

            for (SaleRecord sale : MockDataProvider.sales()) {
                if (sale.daysAgo >= days) {
                    continue;
                }
                Product product = MockDataProvider.findProductById(sale.productId);
                if (product == null) {
                    continue;
                }
                long amount = (long) product.price * sale.units;
                int[] aggregated = byProduct.get(product.id);
                if (aggregated == null) {
                    aggregated = new int[2];
                    byProduct.put(product.id, aggregated);
                }
                aggregated[0] += sale.units;
                aggregated[1] += (int) amount;
                revenue += amount;
                units += sale.units;
                daily[days - 1 - sale.daysAgo] += amount;
            }

            List<ReportRow> rows = new ArrayList<>();
            for (Map.Entry<String, int[]> entry : byProduct.entrySet()) {
                Product product = MockDataProvider.findProductById(entry.getKey());
                if (product == null) {
                    continue;
                }
                rows.add(new ReportRow(product.name, product.region, entry.getValue()[0],
                        entry.getValue()[1]));
            }
            rows.sort((a, b) -> Long.compare(b.revenue, a.revenue));

            return UserRepository.Result.success(new ReportData(days, rows, revenue, units, daily));
        }, callback);
    }

    /** Construye el CSV del reporte para compartir (A7). */
    public String buildCsv(ReportData data) {
        StringBuilder csv = new StringBuilder();
        csv.append("Producto;Region;Unidades;Ingresos COP").append('\n');
        for (ReportRow row : data.rows) {
            csv.append(row.productName.replace(";", ","))
               .append(';').append(row.region)
               .append(';').append(row.units)
               .append(';').append(row.revenue)
               .append('\n');
        }
        csv.append("TOTAL;;").append(data.totalUnits)
           .append(';').append(data.totalRevenue);
        return csv.toString();
    }
}
