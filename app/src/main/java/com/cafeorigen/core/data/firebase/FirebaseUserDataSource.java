package com.cafeorigen.core.data.firebase;

import android.content.Context;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CafeOrigen — Perfiles de usuario en Cloud Firestore (users/{uid}).
 *
 * El alta desde el panel del administrador necesita una cuenta de
 * Authentication. El SDK cliente no permite crearla sin cerrar la sesión
 * actual, así que se usa una instancia secundaria de FirebaseApp: el
 * administrador sigue autenticado y la cuenta nueva nace con su perfil.
 * En producción esto se movería a una Cloud Function con Admin SDK.
 */
public final class FirebaseUserDataSource {

    private static final String PROVISIONING_APP = "cafeorigen-provisioning";

    private static final FirebaseUserDataSource INSTANCE = new FirebaseUserDataSource();

    public static FirebaseUserDataSource get() {
        return INSTANCE;
    }

    private FirebaseUserDataSource() {
    }

    // ==================== PERFIL ====================

    public void fetchProfile(final String uid, final UserRepository.Callback<User> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.USERS).document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    User user = FirebaseMappers.toUser(task.getResult());
                    if (user == null) {
                        callback.onResult(UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.PROFILE_MISSING));
                        return;
                    }
                    FirestoreCache.put(user);
                    callback.onResult(UserRepository.Result.success(user));
                });
    }

    /** Actualiza nombre y teléfono (perfil del vendedor y del comprador). */
    public void updateProfile(final String userId, final String name, final String phone,
                              final UserRepository.Callback<User> callback) {
        final String key = "user:update:" + userId;
        if (!InFlightGuard.begin(key)) {
            callback.onResult(UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        Map<String, Object> change = new HashMap<>();
        change.put(FirestoreSchema.NAME, name);
        change.put(FirestoreSchema.PHONE, phone == null ? "" : phone);
        change.put(FirestoreSchema.UPDATED_AT, FieldValue.serverTimestamp());
        final DocumentReference ref = FirebaseBootstrap.db()
                .collection(FirestoreSchema.USERS).document(userId);
        ref.update(change).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<User>failure(
                        FirebaseErrorMapper.map(task.getException())));
                return;
            }
            ref.get().addOnCompleteListener(read -> {
                InFlightGuard.end(key);
                if (!read.isSuccessful()) {
                    callback.onResult(UserRepository.Result.<User>failure(
                            FirebaseErrorMapper.map(read.getException())));
                    return;
                }
                User user = FirebaseMappers.toUser(read.getResult());
                if (user != null) {
                    FirestoreCache.put(user);
                }
                callback.onResult(user == null
                        ? UserRepository.Result.<User>failure("not_found")
                        : UserRepository.Result.success(user));
            });
        });
    }

    // ==================== ADMINISTRACIÓN ====================

    public void listUsers(final UserRepository.Callback<List<User>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.USERS).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        User user = FirebaseMappers.toUser(doc);
                        if (user != null) {
                            FirestoreCache.put(user);
                            users.add(user);
                        }
                    }
                    Collections.sort(users, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    callback.onResult(UserRepository.Result.success(users));
                });
    }

    /** Alta (isNew) o edición de un usuario desde el panel del administrador. */
    public void saveUser(final User draft, final boolean isNew,
                         final UserRepository.Callback<User> callback) {
        final String key = "user:save:" + (isNew ? "new:" + normalize(draft.email) : draft.id);
        if (!InFlightGuard.begin(key)) {
            callback.onResult(UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        emailInUse(normalize(draft.email), isNew ? null : draft.id, inUse -> {
            if (inUse == null) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<User>failure(
                        FirebaseErrorMapper.NETWORK));
                return;
            }
            if (inUse) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<User>failure("existing_email"));
                return;
            }
            if (isNew) {
                createAccount(draft, callback, key);
            } else {
                updateExisting(draft, callback, key);
            }
        });
    }

    private void updateExisting(final User draft, final UserRepository.Callback<User> callback,
                                final String key) {
        Map<String, Object> change = new HashMap<>();
        change.put(FirestoreSchema.NAME, draft.name);
        change.put(FirestoreSchema.EMAIL, normalize(draft.email));
        change.put(FirestoreSchema.PHONE, draft.phone == null ? "" : draft.phone);
        change.put(FirestoreSchema.ROLE, draft.role == null ? Role.COMPRADOR.name()
                : draft.role.name());
        change.put(FirestoreSchema.ACTIVE, draft.active);
        change.put(FirestoreSchema.UPDATED_AT, FieldValue.serverTimestamp());
        final DocumentReference ref = FirebaseBootstrap.db()
                .collection(FirestoreSchema.USERS).document(draft.id);
        ref.update(change).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<User>failure(
                        FirebaseErrorMapper.map(task.getException())));
                return;
            }
            ref.get().addOnCompleteListener(read -> {
                InFlightGuard.end(key);
                if (!read.isSuccessful()) {
                    callback.onResult(UserRepository.Result.<User>failure(
                            FirebaseErrorMapper.map(read.getException())));
                    return;
                }
                User user = FirebaseMappers.toUser(read.getResult());
                if (user != null) {
                    FirestoreCache.put(user);
                }
                callback.onResult(user == null
                        ? UserRepository.Result.<User>failure("not_found")
                        : UserRepository.Result.success(user));
            });
        });
    }

    /**
     * Crea la cuenta de Authentication en una FirebaseApp secundaria (para no
     * perder la sesión del administrador) y luego su perfil en Firestore.
     */
    private void createAccount(final User draft, final UserRepository.Callback<User> callback,
                               final String key) {
        if (draft.password == null || draft.password.length() < 6) {
            InFlightGuard.end(key);
            callback.onResult(UserRepository.Result.<User>failure("short_password"));
            return;
        }
        final String email = normalize(draft.email);
        final Context context = FirebaseBootstrap.context();
        final FirebaseAuth provisioningAuth;
        try {
            FirebaseApp primary = FirebaseApp.getInstance();
            FirebaseApp provisioning;
            try {
                provisioning = FirebaseApp.getInstance(PROVISIONING_APP);
            } catch (IllegalStateException notInitialized) {
                provisioning = FirebaseApp.initializeApp(context, primary.getOptions(),
                        PROVISIONING_APP);
            }
            if (provisioning == null) {
                InFlightGuard.end(key);
                callback.onResult(UserRepository.Result.<User>failure(
                        FirebaseErrorMapper.UNKNOWN));
                return;
            }
            provisioningAuth = FirebaseAuth.getInstance(provisioning);
        } catch (Throwable error) {
            InFlightGuard.end(key);
            callback.onResult(UserRepository.Result.<User>failure(
                    FirebaseErrorMapper.map(error)));
            return;
        }

        provisioningAuth.createUserWithEmailAndPassword(email, draft.password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        InFlightGuard.end(key);
                        callback.onResult(UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    final FirebaseUser created = task.getResult().getUser();
                    if (created == null) {
                        InFlightGuard.end(key);
                        callback.onResult(UserRepository.Result.<User>failure(
                                FirebaseErrorMapper.UNKNOWN));
                        return;
                    }
                    Role role = draft.role == null ? Role.COMPRADOR : draft.role;
                    Map<String, Object> profile = new HashMap<>();
                    profile.put(FirestoreSchema.NAME, draft.name);
                    profile.put(FirestoreSchema.EMAIL, email);
                    profile.put(FirestoreSchema.PHONE, draft.phone == null ? "" : draft.phone);
                    profile.put(FirestoreSchema.ROLE, role.name());
                    profile.put(FirestoreSchema.ACTIVE, draft.active);
                    profile.put("createdAt", FieldValue.serverTimestamp());

                    FirebaseBootstrap.db().collection(FirestoreSchema.USERS)
                            .document(created.getUid())
                            .set(profile)
                            .addOnCompleteListener(write -> {
                                InFlightGuard.end(key);
                                provisioningAuth.signOut();
                                if (!write.isSuccessful()) {
                                    created.delete();
                                    callback.onResult(UserRepository.Result.<User>failure(
                                            FirebaseErrorMapper.map(write.getException())));
                                    return;
                                }
                                User user = new User(created.getUid(), draft.name, email,
                                        draft.phone == null ? "" : draft.phone, "", role,
                                        draft.active);
                                FirestoreCache.put(user);
                                callback.onResult(UserRepository.Result.success(user));
                            });
                });
    }

    /** Borrado lógico (active = false): la cuenta deja de poder ingresar. */
    public void setUserActive(final String userId, final boolean active,
                              final UserRepository.Callback<User> callback) {
        final String key = "user:active:" + userId;
        if (!InFlightGuard.begin(key)) {
            callback.onResult(UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        final DocumentReference ref = FirebaseBootstrap.db()
                .collection(FirestoreSchema.USERS).document(userId);
        Map<String, Object> change = new HashMap<>();
        change.put(FirestoreSchema.ACTIVE, active);
        change.put(FirestoreSchema.UPDATED_AT, FieldValue.serverTimestamp());
        ref.update(change).addOnCompleteListener(task -> {
            InFlightGuard.end(key);
            if (!task.isSuccessful()) {
                callback.onResult(UserRepository.Result.<User>failure(
                        FirebaseErrorMapper.map(task.getException())));
                return;
            }
            User cached = FirestoreCache.user(userId);
            if (cached != null) {
                User updated = new User(cached.id, cached.name, cached.email, cached.phone,
                        "", cached.role, active);
                FirestoreCache.put(updated);
                callback.onResult(UserRepository.Result.success(updated));
            } else {
                fetchProfile(userId, callback);
            }
        });
    }

    /** Comprueba si otro usuario ya usa ese correo (null = error de red). */
    private void emailInUse(final String email, final String excludingId,
                            final java.util.function.Consumer<Boolean> result) {
        FirebaseBootstrap.db().collection(FirestoreSchema.USERS)
                .whereEqualTo(FirestoreSchema.EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        result.accept(null);
                        return;
                    }
                    QuerySnapshot snapshot = task.getResult();
                    boolean inUse = false;
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (excludingId == null || !excludingId.equals(doc.getId())) {
                                inUse = true;
                                break;
                            }
                        }
                    }
                    result.accept(inUse);
                });
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }
}
