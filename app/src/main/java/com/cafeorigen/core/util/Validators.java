package com.cafeorigen.core.util;

import android.util.Patterns;

/**
 * CafeOrigen — Validaciones de formularios compartidas por las pantallas de
 * autenticación (login, registro, recuperación). Reutilizables para los
 * formularios de usuario de la fase de pantallas por rol.
 */
public final class Validators {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private Validators() {
    }

    /** Trim + revisión de vacío. */
    public static boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Formato de correo con el validador oficial del sistema. */
    public static boolean isValidEmail(String email) {
        return notEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /** Contraseña de longitud mínima configurable en la política de la app. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /** Código de verificación de 6 dígitos numéricos. */
    public static boolean isValid6DigitCode(String code) {
        if (code == null || code.length() != 6) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
