package com.cafeorigen.core.data;

import androidx.annotation.VisibleForTesting;

import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.SaleRecord;
import com.cafeorigen.core.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CafeOrigen — Fuente de datos mock en memoria (sin backend).
 * Contiene usuarios, productos, pedidos, ventas y operaciones CRUD simuladas.
 * Los productos y pedidos se filtran por vendorId (dueño) para respetar los
 * permisos del Vendedor; al integrar backend solo cambia esta clase.
 */
public final class MockDataProvider {

    private static final List<User> USERS = new CopyOnWriteArrayList<>();
    private static final List<Product> PRODUCTS = new CopyOnWriteArrayList<>();
    private static final List<Order> ORDERS = new CopyOnWriteArrayList<>();
    private static final List<SaleRecord> SALES = new CopyOnWriteArrayList<>();
    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private MockDataProvider() {
    }

    static {
        // ============ Usuarios de prueba ============
        USERS.add(new User("U-1001", "Camila Restrepo", "comprador@cafeorigen.co", "3001112233",
                "cafe1234", Role.COMPRADOR));
        USERS.add(new User("U-1002", "Diego Torres · Finca El Mirador", "vendedor@cafeorigen.co", "3004445566",
                "vend1234", Role.VENDEDOR));
        USERS.add(new User("U-1003", "Laura Gómez", "admin@cafeorigen.co", "3007778899",
                "admin1234", Role.ADMINISTRADOR));
        USERS.add(new User("U-1004", "Andrés Plata", "a.plata@micorreo.co", "3012345678",
                "plata1234", Role.COMPRADOR, false));
        // Segundo vendedor para verificar aislamiento por vendorId.
        USERS.add(new User("U-1005", "Marta Ruiz · Finca Betania", "betania@cafeorigen.co", "3005556677",
                "betania1234", Role.VENDEDOR));

        // ============ Productos (café de especialidad) ============
        PRODUCTS.add(new Product("P-101", "U-1002", "Finca El Mirador · Diego Torres",
                "Huila · Supremo Lavado", "Huila", "Caturra", "Lavado", 1750, 38000, 82,
                "Notas a chocolate blanco, panela y naranja. Taza dulce y balanceada, "
                        + "ideal para espresso y métodos de filtrado.",
                null, ProductCategory.CAFE_GRANO, ProductStatus.ACTIVO));
        PRODUCTS.add(new Product("P-102", "U-1005", "Finca Betania · Marta Ruiz",
                "Nariño · Reserva Natural", "Nariño", "Pink Bourbon", "Natural", 1950, 52000, 34,
                "Fermentación natural de 48 horas. Cuerpo cremoso con notas a fruta "
                        + "roja y cacao.",
                null, ProductCategory.CAFE_GRANO, ProductStatus.ACTIVO));
        PRODUCTS.add(new Product("P-103", "U-1002", "Vereda La Chapa · Luis Ortega",
                "Cauca · Honey Selecto", "Cauca", "Castillo", "Honey", 1680, 44500, 56,
                "Mucílago conservado en secado lento. Perfil a panela, melocotón "
                        + "y acidez cítrica suave.",
                null, ProductCategory.CAFE_MOLIDO, ProductStatus.PENDIENTE));
        PRODUCTS.add(new Product("P-104", "U-1005", "Finca El Sol · Grupo Andino",
                "Sierra Nevada · Rainforest", "Magdalena", "Typica", "Lavado", 1450, 47000, 18,
                "Cultivado bajo sombra nativa certificada. Notas a almendra tostada "
                        + "y caña de azúcar.",
                null, ProductCategory.CAFE_VERDE, ProductStatus.PENDIENTE));
        PRODUCTS.add(new Product("P-105", "U-1005", "Finca Betania · Marta Ruiz",
                "Nariño · Geisha Edición Limitada", "Nariño", "Geisha", "Lavado", 2050, 88000, 9,
                "Lote micro de Geisha. Jasmín, bergamota y azúcar moreno; "
                        + "potencia aromática de puntuación 90+.",
                null, ProductCategory.CAFE_GRANO, ProductStatus.ACTIVO));
        PRODUCTS.add(new Product("P-106", "U-1002", "Finca El Mirador · Diego Torres",
                "Huila · Peaberry Panela", "Huila", "Java", "Natural", 1800, 41000, 12,
                "Granos pepita de calidad café. Secado en marquesina, perfil dulce "
                        + "de panela y maní.",
                null, ProductCategory.CAFE_GRANO, ProductStatus.BORRADOR));
        PRODUCTS.add(new Product("P-107", "U-1002", "Café Tolima Verde · Rubén Lozano",
                "Tolima · Descafeinado", "Tolima", "Castillo", "Lavado", 1600, 39500, 0,
                "Descafeinado con agua de caña, sin solventes químicos. Base de "
                        + "caramelo y nueces.",
                null, ProductCategory.CAFE_MOLIDO, ProductStatus.RECHAZADO));
        PRODUCTS.add(new Product("P-108", "U-1002", "Finca El Mirador · Diego Torres",
                "Kit Cata: Vaso + Filtro", null, null, null, 0, 25000, 40,
                "Kit de degustación con vaso cámara y 50 filtros de papel instruidos.",
                null, ProductCategory.ACCESORIOS, ProductStatus.ACTIVO));

        // ============ Pedidos mock (vendorId = vendedor dueño, buyerId = comprador) ============
        List<OrderItem> items1 = Arrays.asList(
                new OrderItem("P-101", "Huila · Supremo Lavado", 4, 38000L * 4),
                new OrderItem("P-108", "Kit Cata: Vaso + Filtro", 1, 25000L));
        ORDERS.add(new Order("O-201", "U-1001", "U-1002", "Camila Restrepo",
                "Cra 45 #23-10, Bogotá", 0, OrderStatus.POR_CONFIRMAR, items1));

        List<OrderItem> items2 = Arrays.asList(
                new OrderItem("P-101", "Huila · Supremo Lavado", 2, 38000L * 2));
        ORDERS.add(new Order("O-202", "U-2001", "U-1002", "Julián Ossa",
                "Calle 8 #15-22, Medellín", 1, OrderStatus.CONFIRMADO, items2));

        List<OrderItem> items3 = Arrays.asList(
                new OrderItem("P-106", "Huila · Peaberry Panela", 6, 41000L * 6));
        ORDERS.add(new Order("O-203", "U-2002", "U-1002", "Natalia Pardo",
                "Av 68 #102-31, Cali", 3, OrderStatus.DESPACHADO, items3));

        List<OrderItem> items4 = Arrays.asList(
                new OrderItem("P-101", "Huila · Supremo Lavado", 3, 38000L * 3),
                new OrderItem("P-108", "Kit Cata: Vaso + Filtro", 2, 25000L * 2));
        ORDERS.add(new Order("O-204", "U-1001", "U-1002", "Camila Restrepo",
                "Cra 45 #23-10, Bogotá", 6, OrderStatus.ENTREGADO, items4));

        List<OrderItem> items5 = Collections.singletonList(
                new OrderItem("P-107", "Tolima · Descafeinado", 5, 39500L * 5));
        ORDERS.add(new Order("O-205", "U-2003", "U-1002", "Óscar Vélez",
                "Cll 70 #4-15, Barranquilla", 9, OrderStatus.CANCELADO, items5));

        // Pedido de OTRO vendedor (debe ser invisible para U-1002)
        List<OrderItem> items6 = Collections.singletonList(
                new OrderItem("P-102", "Nariño · Reserva Natural", 2, 52000L * 2));
        ORDERS.add(new Order("O-206", "U-1001", "U-1005", "Camila Restrepo",
                "Cra 45 #23-10, Bogotá", 1, OrderStatus.CONFIRMADO, items6));

        // ============ Ventas mock (últimos 30 días, deterministas) ============
        for (int day = 0; day < 30; day++) {
            for (int p = 0; p < PRODUCTS.size(); p++) {
                Product product = PRODUCTS.get(p);
                if (product.status == ProductStatus.ACTIVO && (day + p) % 3 == 0) {
                    int units = 1 + ((day * (p + 2)) % 4);
                    SALES.add(new SaleRecord(product.id, day, units));
                }
            }
        }
    }

    // ================== USUARIOS ==================

    /** Lista visible de usuarios (solo lectura). */
    public static List<User> users() {
        return Collections.unmodifiableList(USERS);
    }

    /** Busca un usuario por correo normalizado. */
    public static User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        String target = email.trim().toLowerCase();
        for (User u : USERS) {
            if (u.normalizedEmail().equals(target)) {
                return u;
            }
        }
        return null;
    }

    /** Busca un usuario por id. */
    public static User findById(String id) {
        for (User u : USERS) {
            if (u.id.equals(id)) {
                return u;
            }
        }
        return null;
    }

    /** Crea un comprador nuevo (registro T6). Siempre rol COMPRADOR por seguridad. */
    public static User addUser(String name, String email, String phone, String password) {
        return addUserWithRole(name, email, phone, password, Role.COMPRADOR);
    }

    /** Creación con rol explícito (usado por el Administrador, A3). */
    public static User addUserWithRole(String name, String email, String phone,
                                       String password, Role role) {
        User user = new User("U-" + SEQ.incrementAndGet(), name, email, phone, password, role);
        USERS.add(user);
        return user;
    }

    /** Actualiza un usuario manteniendo su id (edición A4). */
    public static boolean updateUser(User updated) {
        for (int index = 0; index < USERS.size(); index++) {
            if (USERS.get(index).id.equals(updated.id)) {
                USERS.set(index, updated);
                return true;
            }
        }
        return false;
    }

    /** El correo puede repetirse solo si es del mismo usuario. */
    public static boolean findByEmailExcluding(String email, String excludingUserId) {
        User found = findByEmail(email);
        return found != null && !found.id.equals(excludingUserId);
    }

    /** Borrado lógico/bloqueo de cuenta (eliminar usuario A5). */
    public static boolean setUserActive(String userId, boolean active) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }
        return updateUser(new User(user.id, user.name, user.email, user.phone,
                user.password, user.role, active));
    }

    /** Cambia la contraseña de un usuario (recuperación T3). */
    public static boolean updatePassword(String email, String newPassword) {
        User user = findByEmail(email);
        if (user == null) {
            return false;
        }
        return updateUser(new User(user.id, user.name, user.email, user.phone,
                newPassword, user.role, user.active));
    }

    // ================== PRODUCTOS ==================

    public static List<Product> products() {
        return Collections.unmodifiableList(PRODUCTS);
    }

    public static Product findProductById(String id) {
        for (Product product : PRODUCTS) {
            if (product.id.equals(id)) {
                return product;
            }
        }
        return null;
    }

    /** Productos cuyo dueño es el vendedor indicado (permisos V1–V5). */
    public static List<Product> productsFor(String vendorId) {
        List<Product> own = new ArrayList<>();
        for (Product product : PRODUCTS) {
            if (product.vendorId.equals(vendorId)) {
                own.add(product);
            }
        }
        return own;
    }

    /** Alta de producto con id autogenerado (V3). */
    public static Product addProduct(Product draft) {
        Product created = new Product("P-" + SEQ.incrementAndGet(), draft.vendorId,
                draft.vendorName, draft.name, draft.region, draft.variety, draft.process,
                draft.altitudeM, draft.price, draft.stock, draft.description, draft.imageUri,
                draft.category, draft.status);
        PRODUCTS.add(created);
        return created;
    }

    /** Actualiza un producto manteniendo su id (V4). Requiere dueño. */
    public static boolean updateProduct(Product updated) {
        for (int index = 0; index < PRODUCTS.size(); index++) {
            Product current = PRODUCTS.get(index);
            if (current.id.equals(updated.id)) {
                // Bloqueo de permisos: solo el dueño puede modificar.
                if (!current.vendorId.equals(updated.vendorId)) {
                    return false;
                }
                PRODUCTS.set(index, updated);
                return true;
            }
        }
        return false;
    }

    /** Moderación: aprobar o rechazar un café pendiente (A6). Sin chequeo de dueño. */
    public static boolean moderateProduct(String productId, ProductStatus newStatus) {
        Product product = findProductById(productId);
        if (product == null) {
            return false;
        }
        PRODUCTS.set(PRODUCTS.indexOf(product), product.withStatus(newStatus));
        return true;
    }

    /** Borrado lógico de producto (V5 / A6): null imagen no aplica aquí. */
    public static boolean removeProduct(String productId) {
        return PRODUCTS.removeIf(product -> product.id.equals(productId));
    }

    // ================== PEDIDOS ==================

    /** Pedidos cuyo vendedor dueño es el indicado (permisos V6/V7). */
    public static List<Order> ordersFor(String vendorId) {
        List<Order> own = new ArrayList<>();
        for (Order order : ORDERS) {
            if (order.vendorId.equals(vendorId)) {
                own.add(order);
            }
        }
        return own;
    }

    /** Busca pedido: el consumo con permisos ocurre en la capa de repositorio. */
    public static Order findOrderById(String id) {
        for (Order order : ORDERS) {
            if (order.id.equals(id)) {
                return order;
            }
        }
        return null;
    }

    /** Actualiza el estado de un pedido manteniendo el orden (V7). */
    public static boolean updateOrderStatus(String orderId, OrderStatus newStatus) {
        for (int index = 0; index < ORDERS.size(); index++) {
            Order order = ORDERS.get(index);
            if (order.id.equals(orderId)) {
                ORDERS.set(index, order.withStatus(newStatus));
                return true;
            }
        }
        return false;
    }

    /** Alta de pedido desde el checkout del comprador (C5). */
    public static Order addOrder(Order draft) {
        Order created = new Order(draft.id, draft.buyerId, draft.vendorId, draft.buyerName,
                draft.address, draft.daysAgo, draft.status, draft.items);
        ORDERS.add(created);
        return created;
    }

    /** Historial de compras del comprador (C6), filtrado por buyerId. */
    public static List<Order> ordersForBuyer(String buyerId) {
        List<Order> own = new ArrayList<>();
        for (Order order : ORDERS) {
            if (order.buyerId != null && order.buyerId.equals(buyerId)) {
                own.add(order);
            }
        }
        return own;
    }

    /** Descuento de existencias tras una compra (C5). Valida disponibilidad. */
    public static boolean decrementStock(String productId, int units) {
        Product product = findProductById(productId);
        if (product == null || units < 1 || product.stock < units) {
            return false;
        }
        PRODUCTS.set(PRODUCTS.indexOf(product), product.withStock(product.stock - units));
        return true;
    }

    // ================== VENTAS ==================

    public static List<SaleRecord> sales() {
        return Collections.unmodifiableList(SALES);
    }

    /** Reinicia el mock (solo pruebas). */
    @VisibleForTesting
    public static void reset() {
        USERS.clear();
        PRODUCTS.clear();
        ORDERS.clear();
        SALES.clear();
        SEQ.set(1000);
    }
}
