package com.cafeorigen.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.model.User;
import com.cafeorigen.databinding.FragmentAdminUserManagementBinding;

import java.util.List;

/**
 * CafeOrigen — Gestión de usuarios (entrada de la sección A2).
 * Resume las cuentas por estado y enlaza con el listado y la creación.
 * No contiene lógica de negocio: todo pasa por AdminUsersViewModel.
 */
public class AdminUserManagementFragment extends Fragment {

    private AdminUsersViewModel viewModel;
    private FragmentAdminUserManagementBinding binding;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(
                getString(R.string.users_management_title));

        viewModel = new ViewModelProvider(this).get(AdminUsersViewModel.class);

        binding.usersManageListRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSub(new AdminUserListFragment()));
        binding.usersManageCreateRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSub(UserFormFragment.newCreate()));

        // El listado completo se usa solo para el resumen (sin filtros).
        viewModel.setRoleFilter(null);
        viewModel.setQuery("");
        viewModel.load();

        viewModel.getVisibleUsers().observe(getViewLifecycleOwner(), this::bindSummary);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.usersManageProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));
    }

    private void bindSummary(List<User> users) {
        int total = users == null ? 0 : users.size();
        int active = 0;
        for (int index = 0; users != null && index < users.size(); index++) {
            if (users.get(index).active) {
                active++;
            }
        }
        binding.usersManageKpiTotal.setValue(String.valueOf(total));
        binding.usersManageKpiActive.setValue(String.valueOf(active));
        binding.usersManageKpiInactive.setValue(String.valueOf(total - active));

        boolean empty = total == 0;
        boolean loadingNow = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        binding.usersManageEmptyState.setVisibility(empty && !loadingNow
                ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.usersManageEmptyState.setTitle(
                    getString(R.string.users_management_empty_title));
            binding.usersManageEmptyState.setMessage(
                    getString(R.string.users_management_empty_msg));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca los contadores al volver de crear, editar o desactivar
        // (la primera carga ya la hizo onViewCreated).
        if (firstResume) {
            firstResume = false;
            return;
        }
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
