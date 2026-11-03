package com.cafeorigen.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.databinding.FragmentBuyerCatalogBinding;

/**
 * CafeOrigen — Catálogo del Comprador (C2).
 * Solo cafés ACTIVOS con existencia; búsqueda en vivo y filtro por categoría.
 */
public class BuyerCatalogFragment extends Fragment implements BuyerProductsAdapter.Listener {

    private static final String ARG_CATEGORY = "category";

    private BuyerCatalogViewModel viewModel;
    private FragmentBuyerCatalogBinding binding;
    private BuyerProductsAdapter adapter;

    /** Entrada prefiltrada desde el inicio (C1 → C2). */
    public static BuyerCatalogFragment newInstance(ProductCategory category) {
        BuyerCatalogFragment fragment = new BuyerCatalogFragment();
        if (category != null) {
            Bundle args = new Bundle();
            args.putInt(ARG_CATEGORY, category.ordinal());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerCatalogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.buyer_catalog_title));

        viewModel = new ViewModelProvider(requireActivity()).get(BuyerCatalogViewModel.class);
        adapter = new BuyerProductsAdapter(this);
        binding.buyerCatalogRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.buyerCatalogRecycler.setAdapter(adapter);

        // Búsqueda por nombre/región en vivo
        binding.buyerCatalogSearchEdit.addTextChangedListener(new android.text.TextWatcher() {
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

        // Filtro por categoría
        binding.buyerCatalogChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setCategory(null);
                return;
            }
            int checked = checkedIds.get(0);
            ProductCategory category = null;
            if (checked == R.id.chip_catalog_grano) {
                category = ProductCategory.CAFE_GRANO;
            } else if (checked == R.id.chip_catalog_molido) {
                category = ProductCategory.CAFE_MOLIDO;
            } else if (checked == R.id.chip_catalog_verde) {
                category = ProductCategory.CAFE_VERDE;
            } else if (checked == R.id.chip_catalog_accesorios) {
                category = ProductCategory.ACCESORIOS;
            }
            viewModel.setCategory(category);
        });

        // Catálogo + estado vacío
        viewModel.getVisibleProducts().observe(getViewLifecycleOwner(), products -> {
            adapter.submitList(products);
            boolean empty = products == null || products.isEmpty();
            binding.buyerCatalogEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.buyerCatalogRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.buyerCatalogEmpty.setTitle(getString(R.string.buyer_catalog_empty_title));
                binding.buyerCatalogEmpty.setMessage(getString(R.string.buyer_catalog_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.buyerCatalogProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        // Prefiltro proveniente del inicio
        ProductCategory preset = null;
        if (getArguments() != null
                && getArguments().containsKey(ARG_CATEGORY)) {
            preset = ProductCategory.values()[getArguments().getInt(ARG_CATEGORY)];
        }
        if (preset != null) {
            selectChip(preset);
            viewModel.setCategory(preset);
        }
        viewModel.load();
    }

    private void selectChip(ProductCategory category) {
        switch (category) {
            case CAFE_GRANO:
                binding.chipCatalogGrano.setChecked(true);
                break;
            case CAFE_MOLIDO:
                binding.chipCatalogMolido.setChecked(true);
                break;
            case CAFE_VERDE:
                binding.chipCatalogVerde.setChecked(true);
                break;
            case ACCESORIOS:
                binding.chipCatalogAccesorios.setChecked(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onOpen(Product product) {
        ((MainActivity) requireActivity()).openSub(
                BuyerProductDetailFragment.newInstance(product.id));
    }

    @Override
    public void onResume() {
        super.onResume();
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
