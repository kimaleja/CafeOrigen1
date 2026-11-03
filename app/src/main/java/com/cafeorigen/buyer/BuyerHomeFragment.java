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
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.databinding.FragmentBuyerHomeBinding;

/**
 * CafeOrigen — Inicio del Comprador (C1).
 * Banner de marca, accesos por categoría (abren catálogo prefiltrado C2),
 * cafés destacados y accesos al carrito / historial.
 */
public class BuyerHomeFragment extends Fragment implements BuyerProductsAdapter.Listener {

    private BuyerHomeViewModel viewModel;
    private FragmentBuyerHomeBinding binding;
    private BuyerProductsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.tab_inicio));

        viewModel = new ViewModelProvider(this).get(BuyerHomeViewModel.class);
        adapter = new BuyerProductsAdapter(this);
        binding.buyerFeaturedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.buyerFeaturedRecycler.setAdapter(adapter);
        binding.buyerFeaturedRecycler.setNestedScrollingEnabled(false);

        String fullName = SessionManager.get(requireContext()).getUserName();
        String name = fullName == null ? "" : (fullName.indexOf('·') > 0
                ? fullName.substring(0, fullName.indexOf('·')).trim() : fullName.trim());
        binding.buyerGreetingText.setText(getString(R.string.dash_greeting_fmt,
                name.isEmpty() ? getString(R.string.role_label_comprador) : name));

        // Accesos por categoría → catálogo prefiltrado (C2)
        binding.chipOpenCafeGrano.setOnClickListener(v -> openCatalog(ProductCategory.CAFE_GRANO));
        binding.chipOpenCafeMolido.setOnClickListener(v -> openCatalog(ProductCategory.CAFE_MOLIDO));
        binding.chipOpenCafeVerde.setOnClickListener(v -> openCatalog(ProductCategory.CAFE_VERDE));
        binding.chipOpenAccesorios.setOnClickListener(v -> openCatalog(ProductCategory.ACCESORIOS));
        binding.buyerSeeAllText.setOnClickListener(v -> openCatalog(null));

        // Accesos rápidos
        binding.buyerOrdersRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootOrders());
        binding.buyerCartRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootCart());

        viewModel.getFeatured().observe(getViewLifecycleOwner(), featured -> {
            boolean empty = featured == null || featured.isEmpty();
            binding.buyerFeaturedEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.buyerFeaturedRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.buyerFeaturedEmpty.setTitle(getString(R.string.buyer_featured_empty_title));
                binding.buyerFeaturedEmpty.setMessage(getString(R.string.buyer_featured_empty_msg));
            } else {
                adapter.submitList(featured);
            }
        });
    }

    private void openCatalog(ProductCategory category) {
        ((MainActivity) requireActivity()).openRootCatalog(category);
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
