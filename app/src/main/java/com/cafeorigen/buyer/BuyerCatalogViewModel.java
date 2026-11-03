package com.cafeorigen.buyer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.BuyerDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel del catálogo del Comprador (C2).
 * Cafés ACTIVOS del mock con búsqueda en vivo y filtro por categoría.
 */
public class BuyerCatalogViewModel extends ViewModel {

    private final BuyerDataRepository repository = BuyerDataRepository.get();

    private final MutableLiveData<List<Product>> visibleProducts =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<Product> fullList = new ArrayList<>();
    private ProductCategory categoryFilter; // null = Todas
    private String query = "";

    public LiveData<List<Product>> getVisibleProducts() {
        return visibleProducts;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load() {
        loading.setValue(true);
        repository.catalog(result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                fullList = new ArrayList<>(result.data);
                applyFilter();
            }
        });
    }

    public void setCategory(ProductCategory category) {
        categoryFilter = category;
        applyFilter();
    }

    public ProductCategory getCategory() {
        return categoryFilter;
    }

    public void setQuery(String newQuery) {
        query = newQuery == null ? "" : newQuery.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        List<Product> filtered = new ArrayList<>();
        for (Product product : fullList) {
            boolean matchesCategory = categoryFilter == null
                    || product.category == categoryFilter;
            boolean matchesQuery = query.isEmpty()
                    || product.name.toLowerCase().contains(query)
                    || (product.region != null && product.region.toLowerCase().contains(query));
            if (matchesCategory && matchesQuery) {
                filtered.add(product);
            }
        }
        visibleProducts.setValue(filtered);
    }
}
