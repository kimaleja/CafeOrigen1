package com.cafeorigen.buyer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.BuyerDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel del inicio del Comprador (C1).
 * Cafés destacados para el carrusel del home.
 */
public class BuyerHomeViewModel extends ViewModel {

    private final BuyerDataRepository repository = BuyerDataRepository.get();

    private final MutableLiveData<List<Product>> featured = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Product>> getFeatured() {
        return featured;
    }

    public void load() {
        repository.featured(4, result -> {
            if (result.isSuccess()) {
                featured.setValue(result.data);
            }
        });
    }
}
