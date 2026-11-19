package com.cafeorigen.core.data.firebase;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CafeOrigen — Autenticación real con Firebase Authentication.
 *
 * Reglas de diseño:
 *  - La contraseña nunca se guarda ni se registra: vive solo en el SDK.
 *  - El rol y el estado de la cuenta se leen de users/{uid} tras autenticar,
 *    nunca de una preferencia local (una preferencia se puede editar).
 *  - El registro público solo crea COMPRADOR; el resto de roles los asigna
 *    un administrador (reglas de Firestore lo imponen).
 */
public final class FirebaseAuthDataSource {

    /** info devuelto por sendRecoveryCode cuando el correo real ya salió. */
    public static final String RECOVERY_EMAIL_SENT = "recovery_email_sent";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final FirebaseAuthDataSource INSTANCE = new FirebaseAuthDataSource();

    public static FirebaseAuthDataSource get() {
        return INSTANCE;
    }

    private FirebaseAuthDataSource() {
    }

    // ==================== LOGIN ====================

    public void authenticate(@NonNull final String email, @NonNull final String password,
                             final UserRepository.Callback<User> callback) {
        final String key = "auth:login:" + normalize(email);
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        FirebaseBootstrap.auth()
                .signInWithEmailAndPassword(normalize(email), password)
                .addOnCompleteListener(task -> {
                    try {
                        if (!task.isSuccessful()) {
                            deliver(callback, UserRepository.Result.<User>failure(
                                    FirebaseErrorMapper.map(task.getException())));
                            return;
                        }
                        loadProfile(task.getResult().getUser(), callback);
                    } finally {
                        InFlightGuard.end(key);
                    }
                });
    }

    /**
     * Tras autenticar, el perfil manda: rol, estado y datos visibles.
     * Si no hay perfil o la cuenta está bloqueada se cierra la sesión.
     */
    private void loadProfile(final FirebaseUser firebaseUser,
                             final UserRepository.Callback<User> callback) {
        if (firebaseUser == null) {
            deliver(callback, UserRepository.Result.<User>failure("invalid_credentials"));
            return;
        }
        FirebaseBootstrap.db().collection(FirestoreSchema.USERS)
                .document(firebaseUser.getUid())
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        FirebaseBootstrap.auth().signOut();
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    User user = FirebaseMappers.toUser(doc);
                    if (user == null) {
                        FirebaseBootstrap.auth().signOut();
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.PROFILE_MISSING));
                        return;
                    }
                    if (!user.active) {
                        FirebaseBootstrap.auth().signOut();
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.DISABLED));
                        return;
                    }
                    FirestoreCache.put(user);
                    touchLastLogin(user.id);
                    deliver(callback, UserRepository.Result.success(user));
                });
    }

    // ==================== REGISTRO ====================

    /** Registro público: siempre crea COMPRADOR activo (regla de negocio). */
    public void register(@NonNull final String name, @NonNull final String email,
                         final String phone, @NonNull final String password,
                         final UserRepository.Callback<User> callback) {
        final String key = "auth:register:" + normalize(email);
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        final String normalized = normalize(email);
        FirebaseBootstrap.auth().createUserWithEmailAndPassword(normalized, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        InFlightGuard.end(key);
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    final FirebaseUser created = task.getResult().getUser();
                    if (created == null) {
                        InFlightGuard.end(key);
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.UNKNOWN));
                        return;
                    }
                    writeProfile(created, name, normalized, phone, Role.COMPRADOR, callback, key);
                });
    }

    /**
     * Crea el documento users/{uid} y, si esa escritura falla, elimina la
     * cuenta de Authentication para no dejar un login sin perfil.
     */
    private void writeProfile(final FirebaseUser authUser, final String name,
                              final String email, final String phone, final Role role,
                              final UserRepository.Callback<User> callback, final String key) {
        final FirebaseFirestore db = FirebaseBootstrap.db();
        final Map<String, Object> profile = new HashMap<>();
        profile.put(FirestoreSchema.NAME, name);
        profile.put(FirestoreSchema.EMAIL, email);
        profile.put(FirestoreSchema.PHONE, phone == null ? "" : phone);
        profile.put(FirestoreSchema.ROLE, role.name());
        profile.put(FirestoreSchema.ACTIVE, true);
        profile.put("createdAt", FieldValue.serverTimestamp());
        profile.put("lastLoginAt", FieldValue.serverTimestamp());

        db.collection(FirestoreSchema.USERS).document(authUser.getUid())
                .set(profile)
                .addOnCompleteListener(task -> {
                    InFlightGuard.end(key);
                    if (!task.isSuccessful()) {
                        authUser.delete();
                        deliver(callback, UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    UserProfileChangeRequest change = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    authUser.updateProfile(change);

                    User user = new User(authUser.getUid(), name, email,
                            phone == null ? "" : phone, "", role, true);
                    FirestoreCache.put(user);
                    deliver(callback, UserRepository.Result.success(user));
                });
    }

    // ==================== RECUPERACIÓN ====================

    /**
     * Envía el correo real de restablecimiento de Firebase Auth.
     * Devuelve el centinela RECOVERY_EMAIL_SENT como "info" para que la UI
     * muestre el mensaje correcto (el enlace llega al correo, no a la app).
     */
    public void sendRecoveryEmail(@NonNull final String email,
                                  final UserRepository.Callback<String> callback) {
        final String key = "auth:recovery:" + normalize(email);
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        FirebaseBootstrap.auth().sendPasswordResetEmail(normalize(email))
                .addOnCompleteListener(task -> {
                    InFlightGuard.end(key);
                    deliver(callback, task.isSuccessful()
                            ? UserRepository.Result.success(RECOVERY_EMAIL_SENT)
                            : UserRepository.Result.<String>failure(
                                    FirebaseErrorMapper.map(task.getException())));
                });
    }

    // ==================== SESIÓN ====================

    /** Cierra la sesión del SDK y el espejo local. */
    public void signOut() {
        if (!FirebaseBootstrap.isConfigured()) {
            return;
        }
        try {
            FirebaseBootstrap.auth().signOut();
        } catch (Throwable ignored) {
            // La sesión local igual se limpia en SessionManager.
        }
        FirestoreCache.clear();
    }

    /** true si el SDK conserva un usuario autenticado. */
    public boolean hasSession() {
        return FirebaseBootstrap.currentUid() != null;
    }

    /** Actualiza la marca de último ingreso sin bloquear el login. */
    private void touchLastLogin(String uid) {
        FirebaseBootstrap.db().collection(FirestoreSchema.USERS).document(uid)
                .update("lastLoginAt", FieldValue.serverTimestamp());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private static <T> void deliver(final UserRepository.Callback<T> callback,
                                    final UserRepository.Result<T> result) {
        MAIN.post(() -> callback.onResult(result));
    }
}
