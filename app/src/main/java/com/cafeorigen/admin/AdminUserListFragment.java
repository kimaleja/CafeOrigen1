package com.cafeorigen.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.cafeorigen.databinding.FragmentAdminUserListBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Listado y gestión de usuarios (A2).
 * Búsqueda, filtros por rol, creación, edición y eliminación con confirmación.
 */
public class AdminUserListFragment extends Fragment implements UsersAdapter.Listener {

    private AdminUsersViewModel viewModel;
    private FragmentAdminUserListBinding binding;
    private UsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.admin_users_title));

        viewModel = new ViewModelProvider(this).get(AdminUsersViewModel.class);
        adapter = new UsersAdapter(this);

        binding.usersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.usersRecycler.setAdapter(adapter);

        // FAB → crear usuario (A3)
        binding.usersFab.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSub(UserFormFragment.newCreate()));

        // Búsqueda por nombre o correo
        binding.usersSearchEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                viewModel.setQuery(String.valueOf(s));
            }
        });

        // Filtro por rol
        binding.usersFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            Role role = null;
            if (checked == R.id.chip_filter_comprador) {
                role = Role.COMPRADOR;
            } else if (checked == R.id.chip_filter_vendedor) {
                role = Role.VENDEDOR;
            } else if (checked == R.id.chip_filter_admin) {
                role = Role.ADMINISTRADOR;
            }
            viewModel.setRoleFilter(role);
        });

        // Estados de operaciones (mensajes una sola vez)
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case SAVED:
                    Snackbar.make(binding.usersFab,
                            getString(R.string.users_saved_fmt, String.valueOf(state.userName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case DELETED:
                    Snackbar.make(binding.usersFab,
                            getString(R.string.users_deleted_fmt, String.valueOf(state.userName)),
                            Snackbar.LENGTH_LONG).show();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.form_saved_error,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });

        // Listado con filtro + estado vacío
        viewModel.getVisibleUsers().observe(getViewLifecycleOwner(), users -> {
            adapter.submitList(users);
            boolean empty = users == null || users.isEmpty();
            binding.usersEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.usersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.usersEmptyState.setTitle(getString(R.string.users_empty_title));
                binding.usersEmptyState.setMessage(getString(R.string.users_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.usersProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.setRoleFilter(null);
        viewModel.load();
    }

    // =========== Acciones de fila (Adapter.Listener) ===========

    @Override
    public void onEdit(User user) {
        ((MainActivity) requireActivity()).openSub(UserFormFragment.newEdit(user.id));
    }

    @Override
    public void onDelete(User user) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.users_delete_title)
                .setMessage(getString(R.string.users_delete_msg_fmt,
                        String.valueOf(user.name)))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.setUserActive(user, false))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca tras crear/editar o eliminar.
        if (viewModel != null) {
            viewModel.load();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
