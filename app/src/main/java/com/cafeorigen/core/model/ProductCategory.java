package com.cafeorigen.core.model;

/**
 * CafeOrigen — Categorías de producto permitidas para el Vendedor.
 * Se usan en el formulario y en el catálogo (fase Comprador).
 */
public enum ProductCategory {
    CAFE_GRANO("Café en grano"),
    CAFE_MOLIDO("Café molido"),
    CAFE_VERDE("Café verde"),
    ACCESORIOS("Accesorios");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProductCategory[] choices() {
        return values();
    }
}
