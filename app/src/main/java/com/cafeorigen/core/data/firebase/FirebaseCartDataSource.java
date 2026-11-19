package com.cafeorigen.core.data.firebase;

import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CafeOrigen — Carrito persistente en Cloud Firestore (carts/{uid}).
 *
 * Los cambios llegan desde la UI de forma síncrona (CartRepository), así que
 * las escrituras son "write-through" con colapso: si el usuario pulsa + cinco
 * veces seguidas se envía una sola escritura con el último estado, no cinco.
 */
public final class FirebaseCartDataSource {

    private static final FirebaseCartDataSource INSTANCE = new FirebaseCartDataSource();

    public static FirebaseCartDataSource get() {
        return INSTANCE;
    }

    private String pendingUid;
    private List<CartRepository.CartItem> pendingItems;
    private boolean writing;

    private FirebaseCartDataSource() {
    }

    /** Carga el carrito guardado del usuario (si no existe, lista vacía). */
    public void load(String uid, UserRepository.Callback<List<CartRepository.CartItem>> callback) {
        if (uid == null) {
            callback.onResult(UserRepository.Result.success(new ArrayList<>()));
            return;
        }
        FirebaseBootstrap.db().collection(FirestoreSchema.CARTS).document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onResult(UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    callback.onResult(UserRepository.Result.success(
                            FirebaseMappers.toCartItems(doc)));
                });
    }

    /** Guarda el estado más reciente; las llamadas seguidas se agrupan. */
    public void save(String uid, List<CartRepository.CartItem> items) {
        if (uid == null) {
            return;
        }
        pendingUid = uid;
        pendingItems = new ArrayList<>(items);
        if (!writing) {
            flush();
        }
    }

    private void flush() {
        if (writing || pendingUid == null || pendingItems == null) {
            return;
        }
        final String uid = pendingUid;
        final List<CartRepository.CartItem> items = pendingItems;
        pendingItems = null;
        writing = true;

        Map<String, Object> data = new HashMap<>(FirebaseMappers.cartToMap(items));
        data.put(FirestoreSchema.UPDATED_AT, FieldValue.serverTimestamp());

        FirebaseBootstrap.db().collection(FirestoreSchema.CARTS).document(uid)
                .set(data)
                .addOnCompleteListener(task -> {
                    writing = false;
                    // Si llegó un estado más nuevo mientras se escribía, se envía ahora.
                    if (pendingItems != null) {
                        flush();
                    }
                });
    }

    /** Descarta escrituras pendientes (cierre de sesión). */
    public void reset() {
        pendingUid = null;
        pendingItems = null;
        writing = false;
    }
}
