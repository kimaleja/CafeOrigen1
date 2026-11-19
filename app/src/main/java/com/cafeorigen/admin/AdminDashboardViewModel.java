package com.cafeorigen.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.AdminDataRepository;

/**
 * CafeOrigen — ViewModel del dashboard del administrador (A1).
 * Carga los indicadores del mock o de Firestore y expone los errores.
 */
public class AdminDashboardViewModel extends ViewModel {

    private final MutableLiveData<AdminDataRepository.DashboardStats> stats =
            new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);

    private final AdminDataRepository repository = AdminDataRepository.get();

    public LiveData<AdminDataRepository.DashboardStats> getStats() {
        return stats;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public void load() {
        repository.dashboardStats(result -> {
            if (result.isSuccess()) {
                error.setValue(false);
                stats.setValue(result.data);
            } else {
                error.setValue(true);
            }
        });
    }
}
