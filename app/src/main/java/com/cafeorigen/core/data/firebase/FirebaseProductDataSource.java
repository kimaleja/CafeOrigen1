package com.cafeorigen.core.data.firebase;

import android.net.Uri;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.core.util.ImageCompressor;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CafeOrigen — Productos en Cloud Firestore + imágenes en Storage.
 *
 * El catálogo solo expone documentos ACTIVO con existencias; el vendedor
 * administra los suyos (vendorId == uid) y el administrador modera el estado.
 * Las reglas del servidor repiten estas restricciones: la validación local es
 * para dar mensajes claros, no la única defensa.
 */
public final class FirebaseProductDataSource {

    private static final FirebaseProductDataSource INSTANCE = new FirebaseProductDataSource();

    public static FirebaseProductDataSource get() {
        return INSTANCE;
    }

    private FirebaseProductDataSource() {
    }

    // ==================== LECTURAS ====================

    /** Catálogo público: ACTIVO y con stock. */
    public void catalog(UserRepository.Callback<List<Product>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS)
                .whereEqualTo(FirestoreSchema.STATUS, ProductStatus.ACTIVO.name())
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        deliver(callback, UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    List<Product> active = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Product product = FirebaseMappers.toProduct(doc);
                        if (product != null && product.stock > 0) {
                            FirestoreCache.put(product);
                            active.add(product);
                        }
                    }
                    deliver(callback, UserRepository.Result.success(active));
                });
    }

    /** Destacados del inicio: primeros N del catálogo. */
    public void featured(final int limit, UserRepository.Callback<List<Product>> callback) {
        catalog(result -> {
            if (!result.isSuccess()) {
                callback.onResult(result);
                return;
            }
            List<Product> all = result.data;
            List<Product> featured = new ArrayList<>(
                    all.size() > limit ? all.subList(0, limit) : all);
            callback.onResult(UserRepository.Result.success(featured));
        });
    }

    /** Productos de un vendedor (dueño). */
    public void listByVendor(String vendorId, UserRepository.Callback<List<Product>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS)
                .whereEqualTo(FirestoreSchema.VENDOR_ID, vendorId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        deliver(callback, UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    List<Product> products = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Product product = FirebaseMappers.toProduct(doc);
                        if (product != null) {
                            FirestoreCache.put(product);
                            products.add(product);
                        }
                    }
                    deliver(callback, UserRepository.Result.success(products));
                });
    }

    /** Todos los productos (administrador, para moderación). */
    public void listAll(UserRepository.Callback<List<Product>> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        deliver(callback, UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    List<Product> products = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Product product = FirebaseMappers.toProduct(doc);
                        if (product != null) {
                            FirestoreCache.put(product);
                            products.add(product);
                        }
                    }
                    deliver(callback, UserRepository.Result.success(products));
                });
    }

    /** Un producto por id (detalle, carrito). */
    public void byId(String productId, UserRepository.Callback<Product> callback) {
        FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS)
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        deliver(callback, UserRepository.Result.failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    Product product = FirebaseMappers.toProduct(task.getResult());
                    if (product == null) {
                        deliver(callback, UserRepository.Result.<Product>failure("not_found"));
                        return;
                    }
                    FirestoreCache.put(product);
                    deliver(callback, UserRepository.Result.success(product));
                });
    }

    // ==================== ESCRITURAS ====================

    public void saveProduct(final Product draft, final boolean isNew,
                            final UserRepository.Callback<Product> callback) {
        final String key = "product:save:" + (isNew ? "new" : draft.id);
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        final String uid = FirebaseBootstrap.currentUid();
        if (uid == null) {
            InFlightGuard.end(key);
            deliver(callback, UserRepository.Result.<Product>failure("session_expired"));
            return;
        }
        final String vendorId = draft.vendorId == null || draft.vendorId.isEmpty()
                ? uid : draft.vendorId;
        final DocumentReference ref = isNew
                ? FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS).document()
                : FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS).document(draft.id);

        if (isLocalUri(draft.imageUri)) {
            uploadImage(vendorId, ref.getId(), draft.imageUri, (url, errorCode) -> {
                if (errorCode != null) {
                    InFlightGuard.end(key);
                    deliver(callback, UserRepository.Result.<Product>failure(errorCode));
                    return;
                }
                persist(draft, isNew, vendorId, ref, url, callback, key);
            });
        } else {
            persist(draft, isNew, vendorId, ref, draft.imageUri, callback, key);
        }
    }

    private void persist(final Product draft, final boolean isNew, final String vendorId,
                         final DocumentReference ref, final String imageUrl,
                         final UserRepository.Callback<Product> callback, final String key) {
        final FirebaseFirestore db = FirebaseBootstrap.db();
        final Runnable write = () -> {
            Product finalProduct = new Product(ref.getId(), vendorId, draft.vendorName,
                    draft.name, draft.region, draft.variety, draft.process, draft.altitudeM,
                    draft.price, draft.stock, draft.description, imageUrl, draft.category,
                    draft.status);
            Map<String, Object> data = FirebaseMappers.productToMap(finalProduct);
            data.put(FirestoreSchema.UPDATED_AT, FieldValue.serverTimestamp());
            if (isNew) {
                data.put(FirestoreSchema.CREATED_AT, FieldValue.serverTimestamp());
            }
            ref.set(data, SetOptions.merge()).addOnCompleteListener(task -> {
                InFlightGuard.end(key);
                if (!task.isSuccessful()) {
                    deliver(callback, UserRepository.Result.<Product>failure(
                            FirebaseErrorMapper.map(task.getException())));
                    return;
                }
                FirestoreCache.put(finalProduct);
                deliver(callback, UserRepository.Result.success(finalProduct));
            });
        };

        if (isNew) {
            write.run();
            return;
        }
        // Edición: verificar propiedad antes de sobrescribir (defensa en cliente;
        // las reglas lo impiden igual en el servidor).
        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                InFlightGuard.end(key);
                deliver(callback, UserRepository.Result.<Product>failure(
                        FirebaseErrorMapper.map(task.getException())));
                return;
            }
            DocumentSnapshot current = task.getResult();
            if (current == null || !current.exists() || !vendorId.equals(
                    current.getString(FirestoreSchema.VENDOR_ID))) {
                InFlightGuard.end(key);
                deliver(callback, UserRepository.Result.<Product>failure(
                        FirebaseErrorMapper.PERMISSION));
                return;
            }
            write.run();
        });
    }

    public void deleteProduct(final String productId,
                              final UserRepository.Callback<Boolean> callback) {
        final String key = "product:delete:" + productId;
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        FirebaseBootstrap.db().collection(FirestoreSchema.PRODUCTS).document(productId)
                .delete()
                .addOnCompleteListener(task -> {
                    InFlightGuard.end(key);
                    if (!task.isSuccessful()) {
                        deliver(callback, UserRepository.Result.<Boolean>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    FirestoreCache.removeProduct(productId);
                    deliver(callback, UserRepository.Result.success(true));
                });
    }

    /** Moderación del administrador: ACTIVO / RECHAZADO. */
    public void moderate(final String productId, final ProductStatus newStatus,
                         final UserRepository.Callback<Product> callback) {
        final String key = "product:moderate:" + productId;
        if (!InFlightGuard.begin(key)) {
            deliver(callback, UserRepository.Result.failure(FirebaseErrorMapper.IN_PROGRESS));
            return;
        }
        final DocumentReference ref = FirebaseBootstrap.db()
                .collection(FirestoreSchema.PRODUCTS).document(productId);
        ref.update(FirestoreSchema.STATUS, newStatus.name())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        InFlightGuard.end(key);
                        deliver(callback, UserRepository.Result.<Product>failure(
                                FirebaseErrorMapper.map(task.getException())));
                        return;
                    }
                    ref.get().addOnCompleteListener(read -> {
                        InFlightGuard.end(key);
                        if (!read.isSuccessful()) {
                            deliver(callback, UserRepository.Result.<Product>failure(
                                    FirebaseErrorMapper.map(read.getException())));
                            return;
                        }
                        Product product = FirebaseMappers.toProduct(read.getResult());
                        if (product != null) {
                            FirestoreCache.put(product);
                        }
                        deliver(callback, UserRepository.Result.success(product));
                    });
                });
    }

    // ==================== STORAGE ====================

    public interface ImageCallback {
        void onResult(String downloadUrl, String errorCode);
    }

    /**
     * Sube la imagen a products/{vendorId}/{productId}.jpg y devuelve la URL
     * de descarga que se guarda en el documento del producto.
     */
    public void uploadImage(final String vendorId, final String productId,
                            final String localUri, final ImageCallback callback) {
        if (vendorId == null || localUri == null) {
            callback.onResult(null, "image_invalid");
            return;
        }
        final File compressed = ImageCompressor.compress(
                FirebaseBootstrap.context(), Uri.parse(localUri), productId);
        final Uri source = compressed != null ? Uri.fromFile(compressed) : Uri.parse(localUri);
        final StorageReference ref = FirebaseBootstrap.storage()
                .getReference()
                .child("products/" + vendorId + "/" + productId + ".jpg");
        ref.putFile(source).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onResult(null, FirebaseErrorMapper.map(task.getException()));
                return;
            }
            ref.getDownloadUrl().addOnCompleteListener(urlTask -> {
                if (!urlTask.isSuccessful() || urlTask.getResult() == null) {
                    callback.onResult(null, FirebaseErrorMapper.map(urlTask.getException()));
                    return;
                }
                callback.onResult(urlTask.getResult().toString(), null);
            });
        });
    }

    private static boolean isLocalUri(String value) {
        return value != null
                && (value.startsWith("content://") || value.startsWith("file://"));
    }

    private static <T> void deliver(UserRepository.Callback<T> callback,
                                    UserRepository.Result<T> result) {
        if (callback != null) {
            callback.onResult(result);
        }
    }
}
