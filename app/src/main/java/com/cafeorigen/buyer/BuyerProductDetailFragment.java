package com.cafeorigen.buyer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.databinding.FragmentBuyerProductDetailBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Detalle de producto del Comprador (C3).
 * Origen del café, stepper de cantidad (≤ stock) y agregar al carrito (C4).
 */
public class BuyerProductDetailFragment extends Fragment {

    private static final String ARG_PRODUCT_ID = "productId";

    private FragmentBuyerProductDetailBinding binding;
    private Product product;
    private int quantity = 1;

    public static BuyerProductDetailFragment newInstance(String productId) {
        BuyerProductDetailFragment fragment = new BuyerProductDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(
                getString(R.string.buyer_product_detail_title));

        if (getArguments() != null) {
            product = MockDataProvider.findProductById(
                    getArguments().getString(ARG_PRODUCT_ID, ""));
        }
        // El comprador solo ve cafés publicados y con existencia.
        if (product == null
                || product.status != com.cafeorigen.core.model.ProductStatus.ACTIVO
                || product.stock <= 0) {
            Snackbar.make(view, R.string.buyer_product_invalid, Snackbar.LENGTH_SHORT).show();
            popBack();
            return;
        }

        binding.productDetailBack.setOnClickListener(v -> popBack());
        bindProduct();
        setupStepper();
    }

    private void bindProduct() {
        binding.productDetailNameText.setText(product.name);
        binding.productDetailVendorText.setText(product.vendorName);
        binding.productDetailPriceText.setText(
                getString(R.string.buyer_cart_unit_price_fmt, product.price));
        binding.productDetailStockText.setText(
                getString(R.string.buyer_stock_fmt_fmt, product.stock));
        binding.productDetailCategoryBadge.setText(product.category == null
                ? "" : product.category.getLabel());
        binding.productDetailDescriptionText.setText(
                product.description == null ? "" : product.description);

        // Meta de origen (solo cafés; los accesorios no la reportan)
        boolean isCoffee = product.category != null
                && product.category != ProductCategory.ACCESORIOS;
        if (isCoffee) {
            binding.productDetailRegionRow.setContent(
                    getString(R.string.buyer_meta_region_fmt,
                            product.region == null ? "—" : product.region), null);
            binding.productDetailRegionRow.showChevron(false);
            binding.productDetailVarietyRow.setContent(
                    getString(R.string.buyer_meta_variety_fmt,
                            product.variety == null ? "—" : product.variety), null);
            binding.productDetailVarietyRow.showChevron(false);
        } else {
            binding.productDetailMetaContainer.setVisibility(View.GONE);
        }

        if (product.imageUri != null && !product.imageUri.isEmpty()) {
            setImage(product.imageUri);
        }

        if (CartRepository.get().unitsOf(product.id) > 0) {
            quantity = CartRepository.get().unitsOf(product.id);
        }
        refreshStepper();
    }

    private void setImage(String imageUri) {
        ImageView imageView = binding.productDetailImage;
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setImageURI(Uri.parse(imageUri));
    }

    private void setupStepper() {
        binding.productDetailMinusButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                refreshStepper();
            }
        });
        binding.productDetailPlusButton.setOnClickListener(v -> {
            if (quantity < product.stock) {
                quantity++;
                refreshStepper();
            } else {
                Snackbar.make(binding.getRoot(), R.string.buyer_qty_invalid,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        binding.productDetailAddButton.setOnClickListener(v -> addToCart());
    }

    private void refreshStepper() {
        binding.productDetailQtyText.setText(String.valueOf(quantity));
        binding.productDetailMinusButton.setEnabled(quantity > 1);
        binding.productDetailPlusButton.setEnabled(quantity < product.stock);
        long lineTotal = (long) product.price * quantity;
        binding.productDetailLineTotalText.setText(
                getString(R.string.buyer_line_total_fmt, MoneyFormat.cop(lineTotal), quantity));
        binding.productDetailAddButton.setText(getString(R.string.action_add_to_cart));
    }

    private void addToCart() {
        boolean added = CartRepository.get().add(product.id, quantity);
        if (added) {
            Snackbar.make(binding.getRoot(), R.string.buyer_added_to_cart,
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.buyer_view_cart, v ->
                            ((MainActivity) requireActivity()).openRootCart())
                    .show();
        } else {
            Snackbar.make(binding.getRoot(), R.string.buyer_qty_invalid,
                    Snackbar.LENGTH_SHORT).show();
        }
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
