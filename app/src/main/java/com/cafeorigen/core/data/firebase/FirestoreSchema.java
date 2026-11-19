package com.cafeorigen.core.data.firebase;

/**
 * CafeOrigen — Esquema de Cloud Firestore.
 *
 * Colecciones y campos en un único lugar para que las reglas de seguridad,
 * los mapeadores y los scripts de carga compartan los mismos nombres.
 *
 * users/{uid}            perfil del usuario (el id es el uid de Firebase Auth)
 * products/{productId}   café publicado por un vendedor (vendorId = uid)
 * orders/{orderId}       pedido de un comprador a un vendedor
 * carts/{uid}            carrito persistente del comprador
 * categories/{key}       catálogo de categorías (enum ProductCategory)
 */
public final class FirestoreSchema {

    public static final String USERS = "users";
    public static final String PRODUCTS = "products";
    public static final String ORDERS = "orders";
    public static final String CARTS = "carts";
    public static final String CATEGORIES = "categories";

    // Campos: usuario
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String ROLE = "role";
    public static final String ACTIVE = "active";

    // Campos: producto
    public static final String VENDOR_ID = "vendorId";
    public static final String VENDOR_NAME = "vendorName";
    public static final String REGION = "region";
    public static final String VARIETY = "variety";
    public static final String PROCESS = "process";
    public static final String ALTITUDE_M = "altitudeM";
    public static final String PRICE = "price";
    public static final String STOCK = "stock";
    public static final String DESCRIPTION = "description";
    public static final String IMAGE_URI = "imageUri";
    public static final String CATEGORY = "category";
    public static final String STATUS = "status";

    // Campos: pedido
    public static final String BUYER_ID = "buyerId";
    public static final String BUYER_NAME = "buyerName";
    public static final String ADDRESS = "address";
    public static final String ITEMS = "items";
    public static final String TOTAL = "total";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";

    // Campos: línea de pedido
    public static final String PRODUCT_ID = "productId";
    public static final String PRODUCT_NAME = "productName";
    public static final String UNITS = "units";
    public static final String SUBTOTAL = "subtotal";

    // Campos: carrito
    public static final String CART_ITEMS = "items";

    private FirestoreSchema() {
    }
}
