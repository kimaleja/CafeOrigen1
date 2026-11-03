package com.cafeorigen.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel de moderación de productos (A6).
 * Filtra por estado y expone acciones de moderación/eliminación.
 */
public class AdminProductsViewModel extends ViewModel {

    public enum OpsStatus { IDLE, APPROVED, REJECTED, DELETED, ERROR }

    public static class OpsState {
        public final OpsStatus status;
        public final String productName;

        OpsState(OpsStatus status, String productName) {
            this.status = status;
            this.productName = productName;
        }
    }

    private final AdminDataRepository repository = AdminDataRepository.get();

    private final MutableLiveData<List<Product>> visibleProducts =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OpsState> opsState =
            new MutableLiveData<>(new OpsState(OpsStatus.IDLE, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<Product> fullList = new ArrayList<>();
    private ProductStatus statusFilter; // null = Todos

    public LiveData<List<Product>> getVisibleProducts() {
        return visibleProducts;
    }

    public LiveData<OpsState> getOpsState() {
        return opsState;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load() {
        loading.setValue(true);
        repository.listProducts(result -> {
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

    private void applyFilter() {
        List<Product> filtered = new ArrayList<>();
        for (Product product : fullList) {
            if (statusFilter == null || product.status == statusFilter) {
                filtered.add(product);
            }
        }
        visibleProducts.setValue(filtered);
    }

    /** Aprobar un café (queda publicado CTRL ACTIVO). */
    public void approve(Product product) {
        moderate(product, ProductStatus.ACTIVO, OpsStatus.APPROVED);
    }

    /** Rechazar un café publicado pendiente. */
    public void reject(Product product) {
        moderate(product, ProductStatus.RECHAZADO, OpsStatus.REJECTED);
    }

    /** Eliminar café (borrado lógico en backend). */
    public void delete(Product product) {
        loading.setValue(true);
        repository.deleteProduct(product.id, result -> {
            loading.setValue(false);
            if (result.isSuccess() && result.data) {
                opsState.setValue(new OpsState(OpsStatus.DELETED, product.name));
                load();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }

    private void moderate(Product product, ProductStatus newStatus, OpsStatus onOk) {
        loading.setValue(true);
        repository.moderateProduct(product.id, newStatus, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                opsState.setValue(new OpsState(onOk, product.name));
                load();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }
}
