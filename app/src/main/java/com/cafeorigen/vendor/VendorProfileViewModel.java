package com.cafeorigen.vendor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.data.VendorDataRepository;
import com.cafeorigen.core.model.User;

/**
 * CafeOrigen — ViewModel del perfil del Vendedor (V8).
 * Edición de nombre y teléfono con validación de resultado.
 */
public class VendorProfileViewModel extends ViewModel {

    public enum OpsStatus { IDLE, SAVED, ERROR }

    private final VendorDataRepository repository = VendorDataRepository.get();

    private final MutableLiveData<User> profile = new MutableLiveData<>(null);
    private final MutableLiveData<OpsStatus> opsState = new MutableLiveData<>(OpsStatus.IDLE);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LiveData<User> getProfile() {
        return profile;
    }

    public LiveData<OpsStatus> getOpsState() {
        return opsState;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** Carga el usuario actual del mock por id de sesión (V8). */
    public void load(String userId) {
        User user = com.cafeorigen.core.data.MockDataProvider.findById(userId);
        if (user != null) {
            profile.setValue(user);
        }
    }

    public void saveProfile(String userId, String name, String phone) {
        loading.setValue(true);
        repository.updateProfile(userId, name, phone, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                profile.setValue(result.data);
                opsState.setValue(OpsStatus.SAVED);
            } else {
                opsState.setValue(OpsStatus.ERROR);
            }
        });
    }
}
