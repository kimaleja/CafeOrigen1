package com.cafeorigen.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.data.firebase.FirebaseAuthDataSource;
import com.cafeorigen.core.data.firebase.FirebaseBootstrap;
import com.cafeorigen.core.model.User;
import com.cafeorigen.databinding.FragmentLoginBinding;

import java.util.concurrent.Executor;

/**
 * CafeOrigen — Inicio de sesión (T2). Valida el formulario, autentica contra
 * el mock y habilita ingreso con huella usando la API oficial AndroidX
 * Biometric (sin simulación; en dispositivos sin biometría ofrece contraseña).
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    /** Evita abrir MainActivity dos veces si el LiveData reemite SUCCESS. */
    private boolean navigationDone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.loginActionButton.setOnClickListener(v ->
                viewModel.login(
                        String.valueOf(binding.loginEmailEdit.getText()),
                        String.valueOf(binding.loginPasswordEdit.getText())));

        // T4: huella con BiometricPrompt oficial
        binding.loginBiometricButton.setOnClickListener(v -> onBiometricClick());

        // Google: integración real fuera del alcance (mock de visual, deshabilitado con aviso)
        binding.loginGoogleButton.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.module_in_progress_placeholder, Toast.LENGTH_SHORT).show());

        binding.loginForgotText.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new PasswordRecoveryFragment()));

        binding.loginRegisterText.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new RegisterFragment()));

        observeState();
    }

    private void observeState() {
        viewModel.getLoginState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case LOADING:
                    binding.loginActionButton.setEnabled(false);
                    clearFieldErrors();
                    break;
                case SUCCESS:
                    if (navigationDone) {
                        return;
                    }
                    // Abre sesión y habilita la huella para el próximo ingreso.
                    SessionManager.get(requireContext()).startSession(state.user);
                    goToMain();
                    break;
                case ERROR:
                    binding.loginActionButton.setEnabled(true);
                    applyError(state.errorCode);
                    break;
                default:
                    // IDLE: nada que reflejar
                    break;
            }
        });
    }

    /** Errores de validación en el campo correspondiente; el resto como toast. */
    private void applyError(String code) {
        if (code == null) {
            return;
        }
        switch (code) {
            case "empty_email":
                binding.loginEmailInput.setError(getString(R.string.login_error_empty_email));
                break;
            case "invalid_email":
                binding.loginEmailInput.setError(getString(R.string.login_error_invalid_email));
                break;
            case "empty_password":
                binding.loginPasswordInput.setError(getString(R.string.login_error_empty_password));
                break;
            case "short_password":
                binding.loginPasswordInput.setError(getString(R.string.login_error_short_password));
                break;
            default:
                Toast.makeText(requireContext(), R.string.login_invalid_credentials, Toast.LENGTH_LONG).show();
        }
    }

    private void clearFieldErrors() {
        binding.loginEmailInput.setError(null);
        binding.loginPasswordInput.setError(null);
    }

    // ============================ Autenticación biométrica (T4) ============================

    /** Detecta soporte y lanza el diálogo del sistema cuando corresponde. */
    private void onBiometricClick() {
        int canAuthenticate = BiometricManager.from(requireContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);

        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(requireContext(), R.string.biometric_no_enrolled, Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // Estados no compatibles: continuar por la vía convencional.
                Toast.makeText(requireContext(), R.string.biometric_unsupported, Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Toast.makeText(requireContext(), R.string.biometric_security_update,
                        Toast.LENGTH_LONG).show();
                break;
            default:
                // BIOMETRIC_STATUS_UNKNOWN u otro estado: se ofrece la vía convencional.
                Toast.makeText(requireContext(), R.string.biometric_unsupported, Toast.LENGTH_SHORT).show();
        }
    }

    /** Diálogo oficial del sistema; exige una sesión previa con contraseña. */
    private void showBiometricPrompt() {
        // Sin sesión previa guardada no hay contra qué validar; se pide contraseña.
        if (SessionManager.get(requireContext()).getLastUserId() == null) {
            Toast.makeText(requireContext(), R.string.biometric_no_session, Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                // Restaura la sesión del último usuario; nunca se simula la huella.
                // En modo Firebase la identidad queda cacheada en SessionManager
                // (el SDK conserva la sesión); en modo mock se usa el seed local.
                SessionManager session = SessionManager.get(requireContext());
                if (FirebaseBootstrap.isConfigured() && !FirebaseAuthDataSource.get().hasSession()) {
                    // La huella es válida, pero el token del SDK ya no: se pide login.
                    Toast.makeText(requireContext(), R.string.biometric_session_expired,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                User lastUser = FirebaseBootstrap.isConfigured()
                        ? session.restoreLastUser()
                        : MockDataProvider.findById(session.getLastUserId());
                if (lastUser != null) {
                    session.startSession(lastUser);
                    Toast.makeText(requireContext(), R.string.biometric_ok, Toast.LENGTH_SHORT).show();
                    goToMain();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                // El diálogo del sistema sigue abierto: se informa y se permite
                // reintentar o continuar con contraseña.
                Toast.makeText(requireContext(), R.string.biometric_failed,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                switch (errorCode) {
                    case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                        // "Usar contraseña": se permanece en el login sin ruido.
                        return;
                    case BiometricPrompt.ERROR_USER_CANCELED:
                    case BiometricPrompt.ERROR_CANCELED:
                        Toast.makeText(requireContext(), R.string.biometric_cancelled,
                                Toast.LENGTH_SHORT).show();
                        return;
                    case BiometricPrompt.ERROR_LOCKOUT:
                        Toast.makeText(requireContext(), R.string.biometric_lockout,
                                Toast.LENGTH_LONG).show();
                        return;
                    case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                        Toast.makeText(requireContext(), R.string.biometric_lockout_permanent,
                                Toast.LENGTH_LONG).show();
                        return;
                    case BiometricPrompt.ERROR_NO_BIOMETRICS:
                        Toast.makeText(requireContext(), R.string.biometric_no_enrolled,
                                Toast.LENGTH_LONG).show();
                        return;
                    default:
                        // HW no disponible, timeout, etc.: mensaje del sistema.
                        Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_use_pwd))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        prompt.authenticate(promptInfo);
    }

    // ============================ Navegación ============================

    private void goToMain() {
        navigationDone = true;
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
