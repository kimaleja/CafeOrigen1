package com.cafeorigen.core.data.firebase;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CafeOrigen — Guarda de operaciones en curso.
 *
 * Evita duplicados cuando el usuario pulsa dos veces "Confirmar pedido",
 * "Guardar" o "Pagar" mientras la petición anterior sigue viajando a Firebase.
 * Es un candado lógico por clave (no bloquea hilos): el segundo intento
 * recibe el código "operation_in_progress" de inmediato.
 */
public final class InFlightGuard {

    private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

    private InFlightGuard() {
    }

    /** true si la operación arrancó; false si ya había una igual en curso. */
    public static boolean begin(String key) {
        return key != null && RUNNING.add(key);
    }

    /** Libera la clave (llamar SIEMPRE en el callback terminal). */
    public static void end(String key) {
        if (key != null) {
            RUNNING.remove(key);
        }
    }

    public static boolean isRunning(String key) {
        return key != null && RUNNING.contains(key);
    }
}
