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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.databinding.FragmentVendorProductsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Listado de productos del Vendedor (V2).
 * Búsqueda, chips de estado, edición (V4) y eliminación con confirmación (V5).
 * Solo accede su propio inventario (vendorId de la sesión).
 */
public class VendorProductsFragment extends Fragment implements VendorProductsAdapter.Listener {

    private VendorProductsViewModel viewModel;
    private FragmentVendorProductsBinding binding;
    private VendorProductsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.vendor_products_title));

        // VM compartido con el formulario (V3/V4) a nivel de Activity
        viewModel = new ViewModelProvider(requireActivity()).get(VendorProductsViewModel.class);
        adapter = new VendorProductsAdapter(this);

        binding.vendorProductsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.vendorProductsRecycler.setAdapter(adapter);

        // FAB → crear producto (V3)
        binding.vendorProductsFab.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSub(VendorProductFormFragment.newCreate()));

        // Búsqueda en vivo
        binding.vendorProductsSearchEdit.addTextChangedListener(new android.text.TextWatcher() {
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

        // Filtro por estado
        binding.vendorProductsFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            ProductStatus status = null;
            if (checked == R.id.chip_vendor_active) {
                status = ProductStatus.ACTIVO;
            } else if (checked == R.id.chip_vendor_pending) {
                status = ProductStatus.PENDIENTE;
            } else if (checked == R.id.chip_vendor_draft) {
                status = ProductStatus.BORRADOR;
            } else if (checked == R.id.chip_vendor_rejected) {
                status = ProductStatus.RECHAZADO;
            }
            viewModel.setStatusFilter(status);
        });

        // Resultados de operaciones (mensajes una sola vez por cambio de estado)
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case CREATED:
                    Snackbar.make(binding.vendorProductsFab,
                            getString(R.string.vendor_created_fmt,
                                    String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case UPDATED:
                    Snackbar.make(binding.vendorProductsFab,
                            getString(R.string.vendor_updated_fmt,
                                    String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case DELETED:
                    Snackbar.make(binding.vendorProductsFab,
                            getString(R.string.vendor_deleted_fmt,
                                    String.valueOf(state.productName)),
                            Snackbar.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.vendor_ops_error,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });

        // Listado + estado vacío
        viewModel.getVisibleProducts().observe(getViewLifecycleOwner(), products -> {
            adapter.submitList(products);
            boolean empty = products == null || products.isEmpty();
            binding.vendorProductsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.vendorProductsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.vendorProductsEmpty.setTitle(getString(R.string.vendor_products_empty_title));
                binding.vendorProductsEmpty.setMessage(getString(R.string.vendor_products_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.vendorProductsProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.setStatusFilter(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recarga histórica a cada regreso (tras create/edit/delete).
        if (viewModel != null) {
            viewModel.load(SessionManager.get(requireContext()).getUserId());
        }
    }

    // =========== Acciones del Adapter.Listener ===========

    @Override
    public void onEdit(Product product) {
        ((MainActivity) requireActivity()).openSub(
                VendorProductFormFragment.newEdit(product.id));
    }

    @Override
    public void onDelete(Product product) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vendor_delete_title)
                .setMessage(getString(R.string.vendor_delete_msg_fmt,
                        String.valueOf(product.name)))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.delete(product))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
