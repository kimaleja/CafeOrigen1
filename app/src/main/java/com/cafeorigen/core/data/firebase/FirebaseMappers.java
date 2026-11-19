package com.cafeorigen.core.data.firebase;

import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CafeOrigen — Conversión entre los modelos de la app y los documentos de
 * Cloud Firestore. Ninguna pantalla conoce los nombres de campo: todo pasa
 * por aquí.
 */
public final class FirebaseMappers {

    private FirebaseMappers() {
    }

    // ==================== USUARIO ====================

    public static User toUser(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Role role = parseRole(doc.getString(FirestoreSchema.ROLE));
        Boolean active = doc.getBoolean(FirestoreSchema.ACTIVE);
        return new User(
                doc.getId(),
                valueOrEmpty(doc.getString(FirestoreSchema.NAME)),
                valueOrEmpty(doc.getString(FirestoreSchema.EMAIL)),
                valueOrEmpty(doc.getString(FirestoreSchema.PHONE)),
                "", // la contraseña nunca viaja ni se guarda en Firestore
                role,
                active == null || active);
    }

    // ==================== PRODUCTO ====================

    public static Product toProduct(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        return new Product(
                doc.getId(),
                valueOrEmpty(doc.getString(FirestoreSchema.VENDOR_ID)),
                valueOrEmpty(doc.getString(FirestoreSchema.VENDOR_NAME)),
                valueOrEmpty(doc.getString(FirestoreSchema.NAME)),
                valueOrEmpty(doc.getString(FirestoreSchema.REGION)),
                valueOrEmpty(doc.getString(FirestoreSchema.VARIETY)),
                valueOrEmpty(doc.getString(FirestoreSchema.PROCESS)),
                (int) asLong(doc.get(FirestoreSchema.ALTITUDE_M)),
                (int) asLong(doc.get(FirestoreSchema.PRICE)),
                (int) asLong(doc.get(FirestoreSchema.STOCK)),
                valueOrEmpty(doc.getString(FirestoreSchema.DESCRIPTION)),
                doc.getString(FirestoreSchema.IMAGE_URI),
                parseCategory(doc.getString(FirestoreSchema.CATEGORY)),
                parseStatus(doc.getString(FirestoreSchema.STATUS)));
    }

    /** Documento de producto (sin createdAt: lo agrega el repositorio). */
    public static Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put(FirestoreSchema.VENDOR_ID, product.vendorId);
        map.put(FirestoreSchema.VENDOR_NAME, product.vendorName);
        map.put(FirestoreSchema.NAME, product.name);
        map.put(FirestoreSchema.REGION, product.region);
        map.put(FirestoreSchema.VARIETY, product.variety);
        map.put(FirestoreSchema.PROCESS, product.process);
        map.put(FirestoreSchema.ALTITUDE_M, product.altitudeM);
        map.put(FirestoreSchema.PRICE, product.price);
        map.put(FirestoreSchema.STOCK, product.stock);
        map.put(FirestoreSchema.DESCRIPTION, product.description);
        map.put(FirestoreSchema.CATEGORY, product.category == null
                ? ProductCategory.CAFE_GRANO.name() : product.category.name());
        map.put(FirestoreSchema.STATUS, product.status == null
                ? ProductStatus.BORRADOR.name() : product.status.name());
        if (product.imageUri != null && !product.imageUri.isEmpty()) {
            map.put(FirestoreSchema.IMAGE_URI, product.imageUri);
        }
        return map;
    }

    // ==================== PEDIDO ====================

    @SuppressWarnings("unchecked")
    public static Order toOrder(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        List<OrderItem> items = new ArrayList<>();
        Object rawItems = doc.get(FirestoreSchema.ITEMS);
        if (rawItems instanceof List) {
            for (Object raw : (List<Object>) rawItems) {
                if (!(raw instanceof Map)) {
                    continue;
                }
                Map<String, Object> line = (Map<String, Object>) raw;
                Object productId = line.get(FirestoreSchema.PRODUCT_ID);
                Object productName = line.get(FirestoreSchema.PRODUCT_NAME);
                items.add(new OrderItem(
                        productId == null ? "" : String.valueOf(productId),
                        productName == null ? "" : String.valueOf(productName),
                        (int) asLong(line.get(FirestoreSchema.UNITS)),
                        asLong(line.get(FirestoreSchema.SUBTOTAL))));
            }
        }
        return new Order(
                doc.getId(),
                valueOrEmpty(doc.getString(FirestoreSchema.BUYER_ID)),
                valueOrEmpty(doc.getString(FirestoreSchema.VENDOR_ID)),
                valueOrEmpty(doc.getString(FirestoreSchema.BUYER_NAME)),
                valueOrEmpty(doc.getString(FirestoreSchema.ADDRESS)),
                daysAgo(doc.getTimestamp(FirestoreSchema.CREATED_AT)),
                parseOrderStatus(doc.getString(FirestoreSchema.STATUS)),
                items);
    }

    /** Documento de pedido; createdAt se define en el repositorio/transacción. */
    public static Map<String, Object> orderToMap(Order order, Object createdAt) {
        Map<String, Object> map = new HashMap<>();
        map.put(FirestoreSchema.BUYER_ID, order.buyerId);
        map.put(FirestoreSchema.VENDOR_ID, order.vendorId);
        map.put(FirestoreSchema.BUYER_NAME, order.buyerName);
        map.put(FirestoreSchema.ADDRESS, order.address);
        map.put(FirestoreSchema.STATUS, order.status.name());
        map.put(FirestoreSchema.TOTAL, order.total);
        map.put(FirestoreSchema.CREATED_AT, createdAt);
        map.put(FirestoreSchema.UPDATED_AT, createdAt);
        List<Map<String, Object>> lines = new ArrayList<>();
        for (OrderItem item : order.items) {
            Map<String, Object> line = new HashMap<>();
            line.put(FirestoreSchema.PRODUCT_ID, item.productId);
            line.put(FirestoreSchema.PRODUCT_NAME, item.productName);
            line.put(FirestoreSchema.UNITS, item.units);
            line.put(FirestoreSchema.SUBTOTAL, item.subtotal);
            lines.add(line);
        }
        map.put(FirestoreSchema.ITEMS, lines);
        return map;
    }

    // ==================== CARRITO ====================

    public static List<CartRepository.CartItem> toCartItems(DocumentSnapshot doc) {
        List<CartRepository.CartItem> items = new ArrayList<>();
        if (doc == null || !doc.exists()) {
            return items;
        }
        Object rawItems = doc.get(FirestoreSchema.CART_ITEMS);
        if (rawItems instanceof List) {
            for (Object raw : (List<?>) rawItems) {
                if (!(raw instanceof Map)) {
                    continue;
                }
                Map<?, ?> line = (Map<?, ?>) raw;
                Object productId = line.get(FirestoreSchema.PRODUCT_ID);
                Object units = line.get(FirestoreSchema.UNITS);
                if (productId != null) {
                    items.add(new CartRepository.CartItem(
                            String.valueOf(productId), (int) asLong(units)));
                }
            }
        }
        return items;
    }

    public static Map<String, Object> cartToMap(List<CartRepository.CartItem> items) {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (CartRepository.CartItem item : items) {
            Map<String, Object> line = new HashMap<>();
            line.put(FirestoreSchema.PRODUCT_ID, item.productId);
            line.put(FirestoreSchema.UNITS, item.units);
            lines.add(line);
        }
        Map<String, Object> map = new HashMap<>();
        map.put(FirestoreSchema.CART_ITEMS, lines);
        return map;
    }

    // ==================== AUXILIARES ====================

    /** Días transcurridos desde createdAt (0 = hoy), como usaba el mock. */
    public static int daysAgo(Timestamp timestamp) {
        if (timestamp == null) {
            return 0;
        }
        long diff = System.currentTimeMillis() - timestamp.toDate().getTime();
        if (diff <= 0) {
            return 0;
        }
        return (int) (diff / 86_400_000L);
    }

    public static long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static Role parseRole(String value) {
        if (value == null) {
            return Role.COMPRADOR;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(java.util.Locale.US));
        } catch (IllegalArgumentException e) {
            return Role.COMPRADOR;
        }
    }

    public static ProductCategory parseCategory(String value) {
        if (value == null) {
            return ProductCategory.CAFE_GRANO;
        }
        try {
            return ProductCategory.valueOf(value.trim().toUpperCase(java.util.Locale.US));
        } catch (IllegalArgumentException e) {
            return ProductCategory.CAFE_GRANO;
        }
    }

    public static ProductStatus parseStatus(String value) {
        if (value == null) {
            return ProductStatus.BORRADOR;
        }
        try {
            return ProductStatus.valueOf(value.trim().toUpperCase(java.util.Locale.US));
        } catch (IllegalArgumentException e) {
            return ProductStatus.BORRADOR;
        }
    }

    public static OrderStatus parseOrderStatus(String value) {
        if (value == null) {
            return OrderStatus.POR_CONFIRMAR;
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(java.util.Locale.US));
        } catch (IllegalArgumentException e) {
            return OrderStatus.POR_CONFIRMAR;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
