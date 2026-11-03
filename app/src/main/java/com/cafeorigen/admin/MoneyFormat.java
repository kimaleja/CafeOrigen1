package com.cafeorigen.admin;

import android.text.format.Formatter;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CafeOrigen — Formato de cifras para la UI (COP y horas), reutilizable por
 * ambos roles administrativos y de ventas.
 */
public final class MoneyFormat {

    private static final NumberFormat COP = NumberFormat.getIntegerInstance(new Locale("es", "CO"));

    private MoneyFormat() {
    }

    /** Formato "$38.000" (sin decimales). */
    public static String cop(long amount) {
        return "$" + COP.format(amount);
    }

    /** Iniciales para avatares (primeras letras de las dos primeras palabras). */
    public static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "·";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int index = 0; index < Math.min(2, parts.length); index++) {
            if (!parts[index].isEmpty()) {
                initials.append(Character.toUpperCase(parts[index].charAt(0)));
            }
        }
        return initials.length() == 0 ? "·" : initials.toString();
    }
}
