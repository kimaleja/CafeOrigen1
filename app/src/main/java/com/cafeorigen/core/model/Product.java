package com.cafeorigen.core.model;

/**
 * CafeOrigen — Modelo de producto (café de especialidad, mock local).
 * Incluye trazabilidad de origen (región, variedad, proceso, altitud),
 * categoría comercial e imagen (uri de galería opcional).
 * El campo vendorId define la propiedad: cada Vendedor solo ve/edita
 * sus propios productos (permisos en la capa de datos).
 */
public final class Product {

    public final String id;
    public final String vendorId;     // permiso: dueño del producto (User.id)
    public final String vendorName;   // etiqueta visible de la finca/vendedor
    public final String name;
    public final String region;
    public final String variety;
    public final String process;
    public final int altitudeM;
    public final int price;           // COP por libra
    public final int stock;           // libras disponibles
    public final String description;  // notas de cata / descripción comercial
    public final String imageUri;     // uri de galería (null = sin imagen)
    public final ProductCategory category;
    public final ProductStatus status;

    public Product(String id, String vendorId, String vendorName, String name, String region,
                   String variety, String process, int altitudeM, int price, int stock,
                   String description, String imageUri, ProductCategory category,
                   ProductStatus status) {
        this.id = id;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.name = name;
        this.region = region;
        this.variety = variety;
        this.process = process;
        this.altitudeM = altitudeM;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.imageUri = imageUri;
        this.category = category;
        this.status = status;
    }

    public Product withStatus(ProductStatus newStatus) {
        return new Product(id, vendorId, vendorName, name, region, variety, process,
                altitudeM, price, stock, description, imageUri, category, newStatus);
    }

    /** Nueva versión del café tras una venta (descuento de existencias). */
    public Product withStock(int newStock) {
        return new Product(id, vendorId, vendorName, name, region, variety, process,
                altitudeM, price, newStock, description, imageUri, category, status);
    }
}
