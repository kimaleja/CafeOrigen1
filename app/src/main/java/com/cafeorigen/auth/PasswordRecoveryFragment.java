package com.cafeorigen.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.R;
import com.cafeorigen.databinding.FragmentPasswordRecoveryBinding;

/**
 * CafeOrigen — Recuperación de contraseña (T3) en dos pasos.
 * Paso 1: correo → envía código. Paso 2: código + nueva contraseña.
 * El código es simulado (mock sin backend) y se muestra por SnackBar/Toast;
 * en producción llegaría por correo y no se revelaría en UI.
 */
public class PasswordRecoveryFragment extends Fragment {

    private FragmentPasswordRecoveryBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPasswordRecoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.recoveryBackButton.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new LoginFragment()));

        // PASO 1
        binding.recoverySendCodeButton.setOnClickListener(v -> viewModel.requestRecoveryCode(
                String.valueOf(binding.recoveryEmailEdit.getText())));

        // PASO 2
        binding.recoveryResetButton.setOnClickListener(v -> viewModel.resetPassword(
                String.valueOf(binding.recoveryCodeEdit.getText()),
                String.valueOf(binding.recoveryPasswordEdit.getText()),
                String.valueOf(binding.recoveryConfirmEdit.getText())));

        observeState();
    }

    private void observeState() {
        viewModel.getRecoveryState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case LOADING:
                    setButtonsEnabled(false);
                    clearFieldErrors();
                    break;
                case SUCCESS:
                    setButtonsEnabled(true);
                    if (state.info != null) {
                        // Código recibido: avanzar al paso 2.
                        Toast.makeText(requireContext(),
                                getString(R.string.recovery_code_sent, state.info),
                                Toast.LENGTH_LONG).show();
                        goStep2();
                    } else {
                        Toast.makeText(requireContext(), R.string.recovery_success, Toast.LENGTH_LONG).show();
                        ((AuthActivity) requireActivity()).showFragment(new LoginFragment());
                    }
                    break;
                case ERROR:
                    setButtonsEnabled(true);
                    applyError(state.errorCode);
                    break;
                default:
                    break;
            }
        });
    }

    private void goStep2() {
        binding.recoveryStep1Group.setVisibility(View.GONE);
        binding.recoveryStep2Group.setVisibility(View.VISIBLE);
        binding.recoverySubtitleText.setText(R.string.recovery_step2_subtitle);
    }

    private void applyError(String code) {
        if (code == null) {
            return;
        }
        switch (code) {
            case "invalid_email":
                binding.recoveryEmailInput.setError(getString(R.string.login_error_invalid_email));
                break;
            case "email_not_found":
                binding.recoveryEmailInput.setError(getString(R.string.recovery_email_not_found));
                // Si el error llegó en el paso 2 (campo oculto), regresa al paso 1.
                binding.recoveryStep1Group.setVisibility(View.VISIBLE);
                binding.recoveryStep2Group.setVisibility(View.GONE);
                break;
            case "code_invalid":
                binding.recoveryCodeInput.setError(getString(R.string.recovery_code_invalid));
                break;
            case "code_wrong":
                binding.recoveryCodeInput.setError(getString(R.string.recovery_code_wrong));
                break;
            case "short_password":
                binding.recoveryPasswordInput.setError(getString(R.string.login_error_short_password));
                break;
            case "password_mismatch":
                binding.recoveryConfirmInput.setError(getString(R.string.register_passwords_mismatch));
                break;
            default:
                Toast.makeText(requireContext(), R.string.login_invalid_credentials, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFieldErrors() {
        binding.recoveryEmailInput.setError(null);
        binding.recoveryCodeInput.setError(null);
        binding.recoveryPasswordInput.setError(null);
        binding.recoveryConfirmInput.setError(null);
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.recoverySendCodeButton.setEnabled(enabled);
        binding.recoveryResetButton.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
