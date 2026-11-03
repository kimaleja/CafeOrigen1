package com.cafeorigen.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;

/**
 * CafeOrigen — ViewModel de autenticación (capa Controlador, MVVM).
 * Expone el estado de login, registro y recuperación como LiveData; valida
 * formularios antes de delegar en el repositorio y nunca toca vistas.
 */
public class AuthViewModel extends ViewModel {

    /** Estados posibles de cada flujo. */
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    /** Estado inmutable expuesto por LiveData. */
    public static class UiState {
        public final Status status;
        public final String errorCode; // null en SUCCESS/LOADING
        public final User user;        // solo en SUCCESS
        public final String info;      // dato adicional (código simulado, flags)

        UiState(Status status, String errorCode, User user, String info) {
            this.status = status;
            this.errorCode = errorCode;
            this.user = user;
            this.info = info;
        }

        public static UiState idle() {
            return new UiState(Status.IDLE, null, null, null);
        }

        public static UiState loading() {
            return new UiState(Status.LOADING, null, null, null);
        }

        public static UiState success(User user, String info) {
            return new UiState(Status.SUCCESS, null, user, info);
        }

        public static UiState error(String errorCode) {
            return new UiState(Status.ERROR, errorCode, null, null);
        }
    }

    private final MutableLiveData<UiState> loginState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<UiState> registerState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<UiState> recoveryState = new MutableLiveData<>(UiState.idle());

    private final UserRepository repository = UserRepository.get();

    public LiveData<UiState> getLoginState() {
        return loginState;
    }

    public LiveData<UiState> getRegisterState() {
        return registerState;
    }

    public LiveData<UiState> getRecoveryState() {
        return recoveryState;
    }

    /** Login T2: valida campos y delega en el repositorio. */
    public void login(String email, String password) {
        if (!Validators.notEmpty(email)) {
            loginState.setValue(UiState.error("empty_email"));
            return;
        }
        if (!Validators.isValidEmail(email)) {
            loginState.setValue(UiState.error("invalid_email"));
            return;
        }
        if (!Validators.notEmpty(password)) {
            loginState.setValue(UiState.error("empty_password"));
            return;
        }
        if (!Validators.isValidPassword(password)) {
            loginState.setValue(UiState.error("short_password"));
            return;
        }
        loginState.setValue(UiState.loading());
        repository.authenticate(email, password, result -> {
            if (result.isSuccess()) {
                loginState.setValue(UiState.success(result.data, null));
            } else {
                loginState.setValue(UiState.error(result.error));
            }
        });
    }

    /** Registro T6: valida y crea el comprador nuevo. */
    public void register(String name, String email, String phone,
                         String password, String confirm) {
        if (!Validators.notEmpty(name)) {
            registerState.setValue(UiState.error("empty_name"));
            return;
        }
        if (!Validators.isValidEmail(email)) {
            registerState.setValue(UiState.error("invalid_email"));
            return;
        }
        if (!Validators.isValidPassword(password)) {
            registerState.setValue(UiState.error("short_password"));
            return;
        }
        if (!password.equals(confirm)) {
            registerState.setValue(UiState.error("password_mismatch"));
            return;
        }
        registerState.setValue(UiState.loading());
        repository.register(name, email, phone, password, result -> {
            if (result.isSuccess()) {
                registerState.setValue(UiState.success(result.data, null));
            } else {
                registerState.setValue(UiState.error(result.error));
            }
        });
    }

    /** Recuperación T3 paso 1: solita el código. */
    public void requestRecoveryCode(String email) {
        if (!Validators.isValidEmail(email)) {
            recoveryState.setValue(UiState.error("invalid_email"));
            return;
        }
        recoveryState.setValue(UiState.loading());
        repository.sendRecoveryCode(email, result -> {
            if (result.isSuccess()) {
                // El código viaja en info para esta demo sin backend.
                recoveryState.setValue(UiState.success(null, result.data));
            } else {
                recoveryState.setValue(UiState.error(result.error));
            }
        });
    }

    /** Recuperación T3 paso 2: valida código y contraseña nueva. */
    public void resetPassword(String newName, String newPassword, String confirmNewPassword) {
        if (!Validators.isValid6DigitCode(newName)) {
            recoveryState.setValue(UiState.error("code_invalid"));
            return;
        }
        if (!Validators.isValidPassword(newPassword)) {
            recoveryState.setValue(UiState.error("short_password"));
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            recoveryState.setValue(UiState.error("password_mismatch"));
            return;
        }
        recoveryState.setValue(UiState.loading());
        repository.resetPassword(newName, newPassword, result -> {
            if (result.isSuccess()) {
                recoveryState.setValue(UiState.success(null, null));
            } else {
                recoveryState.setValue(UiState.error(result.error));
            }
        });
    }
}
