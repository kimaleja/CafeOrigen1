package com.cafeorigen.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.AdminDataRepository;

/**
 * CafeOrigen — ViewModel del reporte de ventas (A7).
 * Recarga el agregado cuando cambia el periodo (7/30/365 días) y expone
 * el error de carga para no confundirlo con un periodo sin ventas.
 */
public class AdminReportViewModel extends ViewModel {

    public static final int PERIOD_7 = 7;
    public static final int PERIOD_30 = 30;
    public static final int PERIOD_365 = 365;

    private final MutableLiveData<AdminDataRepository.ReportData> report =
            new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> period = new MutableLiveData<>(PERIOD_30);

    private final AdminDataRepository repository = AdminDataRepository.get();

    public LiveData<AdminDataRepository.ReportData> getReport() {
        return report;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Integer> getPeriod() {
        return period;
    }

    public void setPeriod(int days) {
        period.setValue(days);
        load();
    }

    public void load() {
        final int days = period.getValue() == null ? PERIOD_30 : period.getValue();
        loading.setValue(true);
        repository.buildReport(days, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                error.setValue(false);
                report.setValue(result.data);
            } else {
                report.setValue(null);
                error.setValue(true);
            }
        });
    }
}
