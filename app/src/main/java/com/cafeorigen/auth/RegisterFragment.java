package com.cafeorigen.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.databinding.FragmentRegisterBinding;

/**
 * CafeOrigen — Registro de usuario (T6). Crea cuentas COMPRADOR, registra el
 * consentimiento de la Ley 1581 de 2012 (fecha automática) y hace auto-login.
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.registerBackButton.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new LoginFragment()));

        binding.registerLoginText.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new LoginFragment()));

        binding.registerActionButton.setOnClickListener(v -> {
            if (!binding.registerTermsCheckbox.isChecked()) {
                // Consentimiento obligatorio (aviso de privacidad Ley 1581)
                binding.registerTermsCheckbox.setError(getString(R.string.register_error_terms));
                return;
            }
            binding.registerTermsCheckbox.setError(null);
            viewModel.register(
                    String.valueOf(binding.registerNameEdit.getText()),
                    String.valueOf(binding.registerEmailEdit.getText()),
                    String.valueOf(binding.registerPhoneEdit.getText()),
                    String.valueOf(binding.registerPasswordEdit.getText()),
                    String.valueOf(binding.registerConfirmEdit.getText()));
        });

        observeState();
    }

    private void observeState() {
        viewModel.getRegisterState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case LOADING:
                    binding.registerActionButton.setEnabled(false);
                    clearFieldErrors();
                    break;
                case SUCCESS:
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
                    SessionManager.get(requireContext()).startSession(state.user);
                    goMain();
                    break;
                case ERROR:
                    binding.registerActionButton.setEnabled(true);
                    applyError(state.errorCode);
                    break;
                default:
                    break;
            }
        });
    }

    private void applyError(String code) {
        if (code == null) {
            return;
        }
        switch (code) {
            case "empty_name":
                binding.registerNameInput.setError(getString(R.string.register_error_name));
                break;
            case "invalid_email":
                binding.registerEmailInput.setError(getString(R.string.login_error_invalid_email));
                break;
            case "short_password":
                binding.registerPasswordInput.setError(getString(R.string.login_error_short_password));
                break;
            case "password_mismatch":
                binding.registerConfirmInput.setError(getString(R.string.register_passwords_mismatch));
                break;
            case "existing_email":
                binding.registerEmailInput.setError(getString(R.string.register_existing_email));
                break;
            default:
                Toast.makeText(requireContext(), R.string.register_error_name, Toast.LENGTH_LONG).show();
        }
    }

    private void clearFieldErrors() {
        binding.registerNameInput.setError(null);
        binding.registerEmailInput.setError(null);
        binding.registerPasswordInput.setError(null);
        binding.registerConfirmInput.setError(null);
    }

    private void goMain() {
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
