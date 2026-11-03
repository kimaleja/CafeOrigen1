package com.cafeorigen.vendor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.data.VendorDataRepository;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.core.model.ProductStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel de gestión de productos del Vendedor (V2–V5).
 * Listado filtrado por vendorId, búsqueda, filtro por estado/categoría y
 * CRUD expuesto como LiveData para snacks y estados vacíos.
 */
public class VendorProductsViewModel extends ViewModel {

    public enum OpsStatus { IDLE, CREATED, UPDATED, DELETED, ERROR }

    public static class OpsState {
        public final OpsStatus status;
        public final String productName;

        OpsState(OpsStatus status, String productName) {
            this.status = status;
            this.productName = productName;
        }
    }

    /** Borrador editado en el formulario (V3/V4). */
    public static class Draft {
        public final String id; // null = nuevo
        public final String name;
        public final String region;
        public final String variety;
        public final String process;
        public final int altitudeM;
        public final long price;
        public final int stock;
        public final String description;
        public final String imageUri;
        public final ProductCategory category;
        public final ProductStatus status;

        public Draft(String id, String name, String region, String variety, String process,
                     int altitudeM, long price, int stock, String description,
                     String imageUri, ProductCategory category, ProductStatus status) {
            this.id = id;
            this.name = name;
            this.region = region;
            this.variety = variety;
            this.process = process;
            this.altitudeM = altitudeM;
            this.price = price;
            this.stock = stock;
            this.description = description;
            this.imageUri = imageUri;
            this.category = category;
            this.status = status;
        }
    }

    private final VendorDataRepository repository = VendorDataRepository.get();

    private final MutableLiveData<List<Product>> visibleProducts =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OpsState> opsState =
            new MutableLiveData<>(new OpsState(OpsStatus.IDLE, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<Product> fullList = new ArrayList<>();
    private ProductStatus statusFilter;     // null = Todos
    private ProductCategory categoryFilter; // null = Todas
    private String query = "";
    private String vendorId;

    public LiveData<List<Product>> getVisibleProducts() {
        return visibleProducts;
    }

    public LiveData<OpsState> getOpsState() {
        return opsState;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load(String vendorId) {
        this.vendorId = vendorId;
        loading.setValue(true);
        repository.listProducts(vendorId, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                fullList = new ArrayList<>(result.data);
                applyFilter();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }

    public void setStatusFilter(ProductStatus status) {
        statusFilter = status;
        applyFilter();
    }

    public void setCategoryFilter(ProductCategory category) {
        categoryFilter = category;
        applyFilter();
    }

    public void setQuery(String newQuery) {
        query = newQuery == null ? "" : newQuery.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        List<Product> filtered = new ArrayList<>();
        for (Product product : fullList) {
            boolean matchesStatus = statusFilter == null || product.status == statusFilter;
            boolean matchesCategory = categoryFilter == null || product.category == categoryFilter;
            boolean matchesQuery = query.isEmpty()
                    || product.name.toLowerCase().contains(query)
                    || (product.region != null && product.region.toLowerCase().contains(query));
            if (matchesStatus && matchesCategory && matchesQuery) {
                filtered.add(product);
            }
        }
        visibleProducts.setValue(filtered);
    }

    /** Crea (V3) o actualiza (V4). Siempre respeta el dueño de la sesión. */
    public void save(Draft draft, String vendorId, String vendorName) {
        loading.setValue(true);
        Product persist;
        if (draft.id == null) {
            // Creación: estado inicial BORRADOR o derivado del botón usado.
            persist = new Product(null, vendorId, vendorName, draft.name, draft.region,
                    draft.variety, draft.process, draft.altitudeM, (int) draft.price,
                    draft.stock, draft.description, draft.imageUri, draft.category, draft.status);
            repository.saveProduct(persist, true, result -> {
                loading.setValue(false);
                if (result.isSuccess()) {
                    opsState.setValue(new OpsState(OpsStatus.CREATED, result.data.name));
                    load(vendorId);
                } else {
                    opsState.setValue(new OpsState(OpsStatus.ERROR, null));
                }
            });
        } else {
            Product current = findByDraftId(draft.id);
            persist = new Product(draft.id, vendorId, vendorName, draft.name, draft.region,
                    draft.variety, draft.process, draft.altitudeM, (int) draft.price,
                    draft.stock, draft.description, draft.imageUri, draft.category, draft.status);
            repository.saveProduct(persist, false, result -> {
                loading.setValue(false);
                if (result.isSuccess()) {
                    opsState.setValue(new OpsState(OpsStatus.UPDATED, result.data.name));
                    load(vendorId);
                } else {
                    opsState.setValue(new OpsState(OpsStatus.ERROR, null));
                }
            });
        }
    }

    private Product findByDraftId(String id) {
        for (Product product : fullList) {
            if (product.id.equals(id)) {
                return product;
            }
        }
        return null;
    }

    /** Busca un producto del vendedor (edición V4) por id ya filtrado. */
    public Product findOwn(String productId) {
        return findByDraftId(productId);
    }

    /** Eliminación V5 (borrado lógico en backend real). */
    public void delete(Product product) {
        if (vendorId == null) {
            return;
        }
        loading.setValue(true);
        repository.deleteProduct(product.id, result -> {
            loading.setValue(false);
            if (result.isSuccess() && result.data) {
                opsState.setValue(new OpsState(OpsStatus.DELETED, product.name));
                load(vendorId);
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }
}
