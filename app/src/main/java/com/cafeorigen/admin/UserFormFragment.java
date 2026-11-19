package com.cafeorigen.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;
import com.cafeorigen.databinding.FragmentAdminUserFormBinding;
import com.google.android.material.textfield.TextInputLayout;

/**
 * CafeOrigen — Formulario de usuario compartido por Crear (A3) y Editar (A4).
 * Lee el argumento userId; si viene, carga la cuenta desde el repositorio
 * (mock o Firestore) y muestra el switch de estado. Las validaciones viven
 * aquí y la persistencia en AdminUsersViewModel.
 */
public class UserFormFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private static final String STATE_ROLE = "selectedRole";

    private FragmentAdminUserFormBinding binding;
    private AdminUsersViewModel viewModel;
    private String userId;   // null = crear
    private Role selectedRole;

    /** Fábrica para modo creación. */
    public static UserFormFragment newCreate() {
        return new UserFormFragment();
    }

    /** Fábrica para modo edición. */
    public static UserFormFragment newEdit(String id) {
        UserFormFragment fragment = new UserFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserFormBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID, null);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(
                getString(userId == null ? R.string.form_title_create : R.string.form_title_edit));
        viewModel = new ViewModelProvider(this).get(AdminUsersViewModel.class);

        setupRoleDropdown();
        setupBack();
        setupSubmit();
        setupErrorClearing();
        observeOps();
        observeLoading();

        // El rol elegido sobrevive a una rotación de pantalla.
        if (savedInstanceState != null) {
            String savedRole = savedInstanceState.getString(STATE_ROLE);
            if (savedRole != null) {
                try {
                    setupRoleDefault(Role.valueOf(savedRole));
                } catch (IllegalArgumentException ignored) {
                    // Rol inválido en el estado guardado: se elige de nuevo.
                }
            }
        }

        if (userId != null) {
            prepareEditMode();
            viewModel.getEditingUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null && user.id != null && user.id.equals(userId)) {
                    fillEditMode(user);
                }
            });
            viewModel.loadUser(userId);
        }
    }

    private void setupRoleDropdown() {
        String[] roles = new String[]{Role.COMPRADOR.getLabel(), Role.VENDEDOR.getLabel(),
                Role.ADMINISTRADOR.getLabel()};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, roles);
        binding.userFormRoleInputEdit.setAdapter(adapter);
        binding.userFormRoleInputEdit.setOnItemClickListener((parent, v, position, id) ->
                selectedRole = Role.values()[position]);
    }

    private void setupBack() {
        binding.userFormBack.setOnClickListener(v -> popBack());
    }

    /** Quita el error de un campo en cuanto la persona empieza a corregirlo. */
    private void setupErrorClearing() {
        clearErrorOnType(binding.userFormNameInput, binding.userFormNameEdit);
        clearErrorOnType(binding.userFormEmailInput, binding.userFormEmailEdit);
        clearErrorOnType(binding.userFormPhoneInput, binding.userFormPhoneEdit);
        clearErrorOnType(binding.userFormPasswordInput, binding.userFormPasswordEdit);
        clearErrorOnType(binding.userFormRoleInput, binding.userFormRoleInputEdit);
    }

    private void clearErrorOnType(TextInputLayout layout, EditText edit) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                layout.setError(null);
            }
        });
    }

    private void setupSubmit() {
        binding.userFormSaveButton.setOnClickListener(v -> submit());
        binding.userFormCancelButton.setOnClickListener(v -> popBack());
    }

    private void submit() {
        if (!validateFields()) {
            return;
        }
        AdminUsersViewModel.Draft draft = new AdminUsersViewModel.Draft(
                String.valueOf(binding.userFormNameEdit.getText()).trim(),
                String.valueOf(binding.userFormEmailEdit.getText()).trim(),
                String.valueOf(binding.userFormPhoneEdit.getText()).trim(),
                // En edición la contraseña no se envía: se gestiona por recuperación.
                userId == null ? String.valueOf(binding.userFormPasswordEdit.getText()) : "",
                selectedRole,
                binding.userFormActiveSwitch.isChecked());

        viewModel.save(draft, userId, userId == null);
    }

    /** Validaciones de formulario con errores por campo. */
    private boolean validateFields() {
        boolean valid = true;
        clearErrors();

        if (!Validators.notEmpty(String.valueOf(binding.userFormNameEdit.getText()))) {
            binding.userFormNameInput.setError(getString(R.string.register_error_name));
            valid = false;
        }
        String email = String.valueOf(binding.userFormEmailEdit.getText()).trim();
        if (!Validators.notEmpty(email)) {
            binding.userFormEmailInput.setError(getString(R.string.login_error_empty_email));
            valid = false;
        } else if (!Validators.isValidEmail(email)) {
            binding.userFormEmailInput.setError(getString(R.string.login_error_invalid_email));
            valid = false;
        }
        String phone = String.valueOf(binding.userFormPhoneEdit.getText()).trim();
        if (Validators.notEmpty(phone) && phone.replaceAll("\\D", "").length() < 7) {
            binding.userFormPhoneInput.setError(getString(R.string.form_invalid_phone));
            valid = false;
        }
        String password = String.valueOf(binding.userFormPasswordEdit.getText());
        if (userId == null) {
            // En creación la contraseña es obligatoria.
            if (!Validators.notEmpty(password)) {
                binding.userFormPasswordInput.setError(getString(R.string.login_error_empty_password));
                valid = false;
            } else if (!Validators.isValidPassword(password)) {
                binding.userFormPasswordInput.setError(getString(R.string.login_error_short_password));
                valid = false;
            }
        }
        if (selectedRole == null) {
            binding.userFormRoleInput.setError(getString(R.string.form_error_role));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        binding.userFormNameInput.setError(null);
        binding.userFormEmailInput.setError(null);
        binding.userFormPhoneInput.setError(null);
        binding.userFormPasswordInput.setError(null);
        binding.userFormRoleInput.setError(null);
    }

    private void setupRoleDefault(Role role) {
        selectedRole = role;
        binding.userFormRoleInputEdit.setText(role.getLabel(), false);
    }

    /**
     * En edición el correo identifica la cuenta (en Firebase, el email de Auth
     * no se puede cambiar desde el panel) y la contraseña se restablece desde
     * el login. Se informa al administrador en lugar de ignorar datos.
     */
    private void prepareEditMode() {
        binding.userFormEmailInput.setEnabled(false);
        binding.userFormEmailInput.setHelperText(getString(R.string.form_email_locked_helper));
        binding.userFormPasswordInput.setVisibility(View.GONE);
        binding.userFormPasswordHint.setVisibility(View.VISIBLE);
        binding.userFormActiveSwitch.setVisibility(View.VISIBLE);
    }

    private void fillEditMode(User user) {
        binding.userFormNameEdit.setText(user.name);
        binding.userFormEmailEdit.setText(user.email);
        binding.userFormPhoneEdit.setText(user.phone);
        setupRoleDefault(user.role);
        binding.userFormActiveSwitch.setChecked(user.active);

        // Un administrador no puede quitarse a sí mismo el acceso.
        boolean editingSelf = user.id != null
                && user.id.equals(SessionManager.get(requireContext()).getUserId());
        if (editingSelf) {
            binding.userFormRoleInput.setEnabled(false);
            binding.userFormActiveSwitch.setEnabled(false);
            binding.userFormSelfNotice.setVisibility(View.VISIBLE);
        }
    }

    private void observeLoading() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            if (binding == null) {
                return;
            }
            binding.userFormProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.userFormSaveButton.setEnabled(!busy);
        });
    }

    private void observeOps() {
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case SAVED:
                    Toast.makeText(requireContext(),
                            getString(R.string.users_saved_fmt, String.valueOf(state.userName)),
                            Toast.LENGTH_SHORT).show();
                    popBack();
                    break;
                case EMAIL_TAKEN:
                    binding.userFormEmailInput.setError(getString(R.string.users_email_taken));
                    break;
                case LOAD_ERROR:
                    Toast.makeText(requireContext(), R.string.form_load_error,
                            Toast.LENGTH_LONG).show();
                    popBack();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.form_saved_error,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });
    }

    private void popBack() {
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedRole != null) {
            outState.putString(STATE_ROLE, selectedRole.name());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
