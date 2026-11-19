package com.cafeorigen.core.data.firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;

/**
 * CafeOrigen — Arranque y detección de la configuración de Firebase.
 *
 * La app puede ejecutarse en dos modos:
 *  - MOCK: no existe app/google-services.json (o el plugin no se aplicó).
 *          Todo sigue funcionando con MockDataProvider, como hasta ahora.
 *  - FIREBASE: el archivo existe, el plugin google-services generó los
 *          recursos @string/google_app_id, etc. y los repositorios usan
 *          Auth + Firestore + Storage reales.
 *
 * Este punto único evita que el resto de la app tenga que saber en qué modo
 * está: los repositorios preguntan isConfigured() y delegan.
 */
public final class FirebaseBootstrap {

    private static Context appContext;
    private static volatile Boolean configured;
    private static boolean firestoreSettingsApplied;

    private FirebaseBootstrap() {
    }

    /** Guarda el contexto de aplicación y recalcula la detección del modo. */
    public static synchronized void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        configured = null;
        firestoreSettingsApplied = false;
        isConfigured();
    }

    /** true si hay un FirebaseApp inicializado con las opciones del proyecto. */
    public static boolean isConfigured() {
        Boolean cached = configured;
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        if (appContext != null) {
            try {
                result = !FirebaseApp.getApps(appContext).isEmpty()
                        || FirebaseApp.initializeApp(appContext) != null;
            } catch (Throwable ignored) {
                // Sin google-services.json no hay opciones por defecto: modo mock.
                result = false;
            }
        }
        configured = result;
        return result;
    }

    /** Contexto de aplicación; null si la capa aún no se inicializó. */
    public static Context context() {
        return appContext;
    }

    public static FirebaseAuth auth() {
        requireConfigured();
        return FirebaseAuth.getInstance();
    }

    /** Firestore con caché local persistente (lecturas offline). */
    public static FirebaseFirestore db() {
        requireConfigured();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        synchronized (FirebaseBootstrap.class) {
            if (!firestoreSettingsApplied) {
                try {
                    applySettings(db);
                } catch (IllegalStateException alreadyUsed) {
                    // Otra llamada ya configuró la instancia: se conserva.
                }
                firestoreSettingsApplied = true;
            }
        }
        return db;
    }

    public static FirebaseStorage storage() {
        requireConfigured();
        return FirebaseStorage.getInstance();
    }

    /** uid autenticado o null (sin lanzar excepciones). */
    public static String currentUid() {
        if (!isConfigured()) {
            return null;
        }
        try {
            return FirebaseAuth.getInstance().getCurrentUser() == null
                    ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Firebase no está configurado: falta app/google-services.json");
        }
    }

    @SuppressWarnings("deprecation")
    private static void applySettings(FirebaseFirestore db) {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                // Persistencia local real: las lecturas sobreviven al cierre de la app.
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }
}
