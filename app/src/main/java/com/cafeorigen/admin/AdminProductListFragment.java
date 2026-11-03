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
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.databinding.FragmentAdminProductsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Gestión y moderación de productos (A6).
 * Filtros por estado, aprobar / rechazar y eliminar con confirmación.
 */
public class AdminProductListFragment extends Fragment implements AdminProductsAdapter.Listener {

    private AdminProductsViewModel viewModel;
    private FragmentAdminProductsBinding binding;
    private AdminProductsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.admin_products_title));

        viewModel = new ViewModelProvider(this).get(AdminProductsViewModel.class);
        adapter = new AdminProductsAdapter(this);

        binding.productsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.productsRecycler.setAdapter(adapter);

        // Filtro por estado
        binding.productsFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            ProductStatus status = null;
            if (checked == R.id.chip_products_pending) {
                status = ProductStatus.PENDIENTE;
            } else if (checked == R.id.chip_products_active) {
                status = ProductStatus.ACTIVO;
            } else if (checked == R.id.chip_products_rejected) {
                status = ProductStatus.RECHAZADO;
            } else if (checked == R.id.chip_products_draft) {
                status = ProductStatus.BORRADOR;
            }
            viewModel.setStatusFilter(status);
        });

        // Operaciones (aprobado / rechazado / eliminado / error)
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case APPROVED:
                    Snackbar.make(binding.productsRecycler,
                            getString(R.string.product_approved_fmt, String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case REJECTED:
                    Snackbar.make(binding.productsRecycler,
                            getString(R.string.product_rejected_fmt, String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case DELETED:
                    Snackbar.make(binding.productsRecycler,
                            getString(R.string.product_deleted_fmt, String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.form_saved_error, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });

        // Listado + estado vacío
        viewModel.getVisibleProducts().observe(getViewLifecycleOwner(), products -> {
            adapter.submitList(products);
            boolean empty = products == null || products.isEmpty();
            binding.productsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.productsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.productsEmptyState.setTitle(getString(R.string.products_empty_title));
                binding.productsEmptyState.setMessage(getString(R.string.products_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.productsProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.setStatusFilter(null);
        viewModel.load();
    }

    // =========== Acciones del Adapter.Listener (A6) ===========

    @Override
    public void onApprove(Product product) {
        viewModel.approve(product);
    }

    @Override
    public void onReject(Product product) {
        viewModel.reject(product);
    }

    @Override
    public void onDelete(Product product) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.product_delete_title)
                .setMessage(getString(R.string.product_delete_msg_fmt,
                        String.valueOf(product.name), String.valueOf(product.vendorName)))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.delete(product))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca al volver de formularios o tras eliminar.
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
