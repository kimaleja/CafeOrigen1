package com.cafeorigen.core.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.cafeorigen.core.data.firebase.FirebaseAuthDataSource;
import com.cafeorigen.core.data.firebase.FirebaseBootstrap;
import com.cafeorigen.core.data.firebase.FirestoreCache;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;

/**
 * CafeOrigen — Gestión de sesión (T1/T2/T4).
 * Persiste la sesión activa, el rol para el control de acceso y la referencia
 * al último usuario autenticado (habilita el ingreso con huella).
 * No almacena contraseñas; en modo Firebase la sesión real vive en el SDK y
 * este archivo solo guarda los datos necesarios para el ruteo inmediato.
 */
public final class SessionManager {

    private static final String PREFS = "cafeorigen_session";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LAST_USER_ID = "last_user_id";
    private static final String KEY_LAST_USER_NAME = "last_user_name";
    private static final String KEY_LAST_USER_EMAIL = "last_user_email";
    private static final String KEY_LAST_ROLE = "last_role";

    private static volatile SessionManager instance;

    private final SharedPreferences prefs;
    private final Context appContext;

    private SessionManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Singleton thread-safe (llamar desde el hilo de UI normalmente). */
    public static SessionManager get(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    /** Abre sesión tras login, registro o huella exitosa. */
    public void startSession(User user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_USER_NAME, user.name)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_ROLE, user.role.asKey())
                .putString(KEY_LAST_USER_ID, user.id) // habilita biometría en el siguiente arranque
                .putString(KEY_LAST_USER_NAME, user.name)
                .putString(KEY_LAST_USER_EMAIL, user.email)
                .putString(KEY_LAST_ROLE, user.role.asKey())
                .apply();
        CartRepository.get().attach(user.id);
    }

    /** Cierra la sesión activa, conservando quién fue el último usuario. */
    public void clearSession() {
        if (FirebaseBootstrap.isConfigured()) {
            FirebaseAuthDataSource.get().signOut();
            FirestoreCache.clear();
        }
        CartRepository.get().detach();
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_ROLE)
                .apply();
    }

    public boolean isLoggedIn() {
        if (prefs.getString(KEY_USER_ID, null) == null) {
            return false;
        }
        // En modo Firebase la sesión del SDK manda: si expiró o se cerró desde
        // otro punto, se vuelve a Login en lugar de abrir MainActivity sin token.
        if (FirebaseBootstrap.isConfigured() && !FirebaseAuthDataSource.get().hasSession()) {
            return false;
        }
        return true;
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    /** Rol con el que se controlan destinos y menús (control de acceso). */
    public Role getRole() {
        return Role.fromKey(prefs.getString(KEY_ROLE, null));
    }

    /** Último usuario autenticado con contraseña (para login por huella). */
    public String getLastUserId() {
        return prefs.getString(KEY_LAST_USER_ID, null);
    }

    /**
     * Reconstruye el último usuario para el ingreso biométrico.
     * No incluye contraseña (nunca se guarda): solo identidad y rol cacheados.
     */
    public User restoreLastUser() {
        String lastId = prefs.getString(KEY_LAST_USER_ID, null);
        if (lastId == null) {
            return null;
        }
        String name = prefs.getString(KEY_LAST_USER_NAME, "");
        String email = prefs.getString(KEY_LAST_USER_EMAIL, "");
        Role role = Role.fromKey(prefs.getString(KEY_LAST_ROLE, null));
        return new User(lastId, name == null ? "" : name, email == null ? "" : email,
                "", "", role, true);
    }
}
