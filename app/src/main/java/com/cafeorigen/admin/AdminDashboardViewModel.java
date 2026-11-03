package com.cafeorigen.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.core.data.UserRepository;

/**
 * CafeOrigen — ViewModel del dashboard del administrador (A1).
 * Carga una única vez los indicadores del mock.
 */
public class AdminDashboardViewModel extends ViewModel {

    private final MutableLiveData<AdminDataRepository.DashboardStats> stats =
            new MutableLiveData<>(null);

    private final AdminDataRepository repository = AdminDataRepository.get();

    public LiveData<AdminDataRepository.DashboardStats> getStats() {
        return stats;
    }

    public void load() {
        repository.dashboardStats(result -> {
            if (result.isSuccess()) {
                stats.setValue(result.data);
            }
        });
    }
}
