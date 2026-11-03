package com.cafeorigen.core.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;

/**
 * CafeOrigen — Gestión de sesión (T1/T2/T4).
 * Persiste la sesión activa, el rol para el control de acceso y la referencia
 * al último usuario autenticado (habilita el ingreso con huella).
 * No almacena contraseñas; en la app real la sesión sería un token del backend.
 */
public final class SessionManager {

    private static final String PREFS = "cafeorigen_session";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LAST_USER_ID = "last_user_id";

    private static volatile SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
                .apply();
    }

    /** Cierra la sesión activa, conservando quién fue el último usuario. */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_ROLE)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_USER_ID, null) != null;
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
}
