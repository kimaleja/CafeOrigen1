package com.cafeorigen.vendor;

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
import com.cafeorigen.core.model.User;
import com.cafeorigen.core.util.Validators;
import com.cafeorigen.databinding.DialogVendorEditProfileBinding;
import com.cafeorigen.databinding.FragmentVendorProfileBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * CafeOrigen — Perfil del Vendedor (V8).
 * Datos personales, finca y certificaciones; edición de nombre/teléfono
 * con validación y cierre de sesión con confirmación.
 */
public class VendorProfileFragment extends Fragment {

    private VendorProfileViewModel viewModel;
    private FragmentVendorProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.vendor_profile_title));

        viewModel = new ViewModelProvider(this).get(VendorProfileViewModel.class);

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::bindProfile);

        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            if (state == VendorProfileViewModel.OpsStatus.SAVED) {
                Toast.makeText(requireContext(), R.string.vendor_profile_saved,
                        Toast.LENGTH_SHORT).show();
                // La sesión muestra el nombre actualizado.
                User updated = viewModel.getProfile().getValue();
                if (updated != null) {
                    SessionManager.get(requireContext()).startSession(updated);
                }
                bindProfile(updated);
            } else if (state == VendorProfileViewModel.OpsStatus.ERROR) {
                Toast.makeText(requireContext(), R.string.vendor_ops_error,
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.vendorProfileEditButton.setOnClickListener(v -> showEditDialog());

        binding.vendorProfileLogoutRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).requestLogout());

        viewModel.load(SessionManager.get(requireContext()).getUserId());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.load(SessionManager.get(requireContext()).getUserId());
        }
    }

    private void bindProfile(User user) {
        if (user == null || binding == null) {
            return;
        }
        String initials = com.cafeorigen.admin.MoneyFormat.initials(splitName(user.name));
        binding.vendorProfileAvatar.setText(initials);
        binding.vendorProfileNameText.setText(splitName(user.name));
        binding.vendorProfileEmailText.setText(user.email);
        String farm = farmName(user.name);
        if (farm != null) {
            binding.vendorProfileFarmText.setVisibility(View.VISIBLE);
            binding.vendorProfileFarmText.setText(farm);
        } else {
            binding.vendorProfileFarmText.setVisibility(View.GONE);
        }
    }

    /** Nombre de la finca: la parte después de «·» en el nombre de sesión. */
    private String farmName(String fullName) {
        int split = fullName.indexOf('·');
        return split > 0 && split < fullName.length() - 1
                ? fullName.substring(split + 1).trim() : null;
    }

    private String splitName(String fullName) {
        int split = fullName.indexOf('·');
        return split > 0 ? fullName.substring(0, split).trim() : fullName.trim();
    }

    /** Diálogo de edición con validación local (V8). */
    private void showEditDialog() {
        User current = viewModel.getProfile().getValue();
        if (current == null) {
            return;
        }
        DialogVendorEditProfileBinding dialogBinding =
                DialogVendorEditProfileBinding.inflate(getLayoutInflater());
        dialogBinding.vendorEditNameEdit.setText(splitName(current.name));
        dialogBinding.vendorEditPhoneEdit.setText(current.phone);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vendor_profile_edit_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = String.valueOf(dialogBinding.vendorEditNameEdit.getText()).trim();
                    String phone = String.valueOf(dialogBinding.vendorEditPhoneEdit.getText()).trim();
                    if (!validate(name, phone, dialogBinding)) {
                        return;
                    }
                    viewModel.saveProfile(SessionManager.get(requireContext()).getUserId(),
                            name, phone);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private boolean validate(String name, String phone,
                             DialogVendorEditProfileBinding dialogBinding) {
        boolean valid = true;
        if (!Validators.notEmpty(name)) {
            dialogBinding.vendorEditNameInput.setError(getString(R.string.vendor_error_name));
            valid = false;
        }
        if (phone.replaceAll("\\D", "").length() < 7) {
            dialogBinding.vendorEditPhoneInput.setError(
                    getString(R.string.vendor_error_phone));
            valid = false;
        }
        return valid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
