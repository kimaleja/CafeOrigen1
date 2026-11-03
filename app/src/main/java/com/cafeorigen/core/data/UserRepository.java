package com.cafeorigen.core.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CafeOrigen — Repositorio de usuarios.
 * Aísla la fuente mock del resto de la app; simula la latencia de red con un
 * executor y entrega los resultados en el hilo principal para LiveData.
 * En la integración con backend solo cambia el cuerpo de cada método.
 */
public final class UserRepository {

    /** Resultado genérico de repositorio (éxito o mensaje de error). */
    public static final class Result<T> {
        public final T data;
        public final String error;

        private Result(T data, String error) {
            this.data = data;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(data, null);
        }

        public static <T> Result<T> failure(String message) {
            return new Result<>(null, message);
        }
    }

    /** Callback entregado en el hilo principal. */
    public interface Callback<T> {
        void onResult(Result<T> result);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long MOCK_LATENCY_MS = 450L;

    private static volatile UserRepository instance;

    public static UserRepository get() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository();
                }
            }
        }
        return instance;
    }

    private UserRepository() {
    }

    /** Autentica credenciales contra el mock (T2). */
    public void authenticate(@NonNull final String email, @NonNull final String password,
                             final Callback<User> callback) {
        EXECUTOR.execute(() -> {
            Result<User> result;
            if (!Validators.isValidEmail(email)) {
                result = Result.failure("invalid_email");
            } else {
                User user = MockDataProvider.findByEmail(email);
                if (user != null && user.password.equals(password)) {
                    result = Result.success(user);
                } else {
                    result = Result.failure("invalid_credentials");
                }
            }
            MAIN.postDelayed(() -> callback.onResult(result), MOCK_LATENCY_MS);
        });
    }

    /** Registra un comprador nuevo (T6). Valida unicidad de correo. */
    public void register(@NonNull final String name, @NonNull final String email,
                         final String phone, @NonNull final String password,
                         final Callback<User> callback) {
        EXECUTOR.execute(() -> {
            Result<User> result;
            if (!Validators.isValidEmail(email)) {
                result = Result.failure("invalid_email");
            } else if (MockDataProvider.findByEmail(email) != null) {
                result = Result.failure("existing_email");
            } else {
                User created = MockDataProvider.addUser(name, email, phone, password);
                result = Result.success(created);
            }
            MAIN.postDelayed(() -> callback.onResult(result), MOCK_LATENCY_MS);
        });
    }

    /** Genera y guarda un código de verificación simulado (T3, paso 1). */
    public void sendRecoveryCode(@NonNull final String email, final Callback<String> callback) {
        EXECUTOR.execute(() -> {
            Result<String> result;
            User user = MockDataProvider.findByEmail(email);
            if (user == null) {
                result = Result.failure("email_not_found");
            } else {
                String code = String.format("%06d", (int) (Math.random() * 1_000_000));
                pendingCode = code;
                pendingRecoveryEmail = user.normalizedEmail();
                result = Result.success(code); // en producción se envía por correo, nunca a la UI
            }
            MAIN.postDelayed(() -> callback.onResult(result), MOCK_LATENCY_MS);
        });
    }

    /** Restablece la contraseña si el código coincide (T3, paso 2). */
    public void resetPassword(@NonNull final String code, @NonNull final String newPassword,
                              final Callback<Boolean> callback) {
        EXECUTOR.execute(() -> {
            Result<Boolean> result;
            if (pendingCode == null || !pendingCode.equals(code.trim())) {
                result = Result.failure("code_wrong");
            } else if (pendingRecoveryEmail == null) {
                result = Result.failure("code_wrong");
            } else {
                boolean updated = MockDataProvider.updatePassword(pendingRecoveryEmail, newPassword);
                if (updated) {
                    pendingCode = null;
                    pendingRecoveryEmail = null;
                    result = Result.success(true);
                } else {
                    result = Result.failure("email_not_found");
                }
            }
            MAIN.postDelayed(() -> callback.onResult(result), MOCK_LATENCY_MS);
        });
    }

    // Estado transitorio del flujo de recuperación (solo en memoria, no persistente)
    private String pendingCode;
    private String pendingRecoveryEmail;
}
