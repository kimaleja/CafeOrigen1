package com.cafeorigen.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;
import com.cafeorigen.databinding.FragmentAdminUserFormBinding;

/**
 * CafeOrigen — Formulario de usuario compartido por Crear (A3) y Editar (A4).
 * Lee el argumento userId; si viene, precarga y muestra el switch de estado.
 * Contiene validaciones de campos en el propio formulario.
 */
public class UserFormFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";

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
        viewModel = new ViewModelProvider(requireActivity()).get(AdminUsersViewModel.class);

        setupRoleDropdown();
        setupBack();
        setupSubmit();
        observeOps();

        if (userId != null) {
            fillEditMode();
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
                String.valueOf(binding.userFormPasswordEdit.getText()),
                selectedRole,
                binding.userFormActiveSwitch.isChecked());

        if (userId == null) {
            viewModel.save(draft, null, true);
        } else {
            viewModel.save(draft, userId, false);
        }
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
        } else if (Validators.notEmpty(password) && !Validators.isValidPassword(password)) {
            // En edición solo se valida si el administrador la cambia.
            binding.userFormPasswordInput.setError(getString(R.string.login_error_short_password));
            valid = false;
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

    private void fillEditMode() {
        User user = MockDataProvider.findById(userId);
        if (user == null) {
            Toast.makeText(requireContext(), R.string.form_saved_error, Toast.LENGTH_SHORT).show();
            popBack();
            return;
        }
        binding.userFormNameEdit.setText(user.name);
        binding.userFormEmailEdit.setText(user.email);
        binding.userFormPhoneEdit.setText(user.phone);
        setupRoleDefault(user.role);
        binding.userFormActiveSwitch.setChecked(user.active);
        binding.userFormActiveSwitch.setVisibility(View.VISIBLE);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
