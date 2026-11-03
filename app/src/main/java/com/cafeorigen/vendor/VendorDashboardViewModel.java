package com.cafeorigen.vendor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.VendorDataRepository;

/**
 * CafeOrigen — ViewModel del panel principal del Vendedor (V1).
 * KPIs, gráfico semanal, stock bajo y pedidos por confirmar; todas las
 * consultas se filtran por vendorId del vendedor en sesión.
 */
public class VendorDashboardViewModel extends ViewModel {

    private final VendorDataRepository repository = VendorDataRepository.get();

    private final MutableLiveData<VendorDataRepository.VendorDashboard> stats =
            new MutableLiveData<>(null);

    public LiveData<VendorDataRepository.VendorDashboard> getStats() {
        return stats;
    }

    public void load(String vendorId) {
        repository.dashboardFor(vendorId, result -> {
            if (result.isSuccess()) {
                stats.setValue(result.data);
            }
        });
    }
}
