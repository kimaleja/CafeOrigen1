package com.cafeorigen.buyer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.databinding.ItemBuyerProductBinding;

/**
 * CafeOrigen — Adaptador de tarjetas de producto del Comprador (C1/C2).
 * Imagen (uri de galería o ícono), precio, categoría y existencias.
 */
public class BuyerProductsAdapter extends ListAdapter<Product, BuyerProductsAdapter.Holder> {

    /** Apertura del detalle (C3) a cargo del Fragment. */
    public interface Listener {
        void onOpen(Product product);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return java.util.Objects.equals(oldItem.name, newItem.name)
                    && oldItem.price == newItem.price
                    && oldItem.stock == newItem.stock
                    && java.util.Objects.equals(oldItem.category, newItem.category);
        }
    };

    public BuyerProductsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuyerProductBinding binding = ItemBuyerProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemBuyerProductBinding binding;

        Holder(ItemBuyerProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            Context context = binding.getRoot().getContext();
            binding.productNameText.setText(product.name);
            binding.productPriceText.setText(MoneyFormat.cop(product.price));
            binding.productCategoryText.setText(product.category == null
                    ? "" : product.category.getLabel());
            binding.productStockText.setText(
                    context.getString(R.string.vendor_stock_fmt, product.stock));
            binding.productVendorText.setText(product.vendorName);

            if (product.imageUri != null && !product.imageUri.isEmpty()) {
                binding.productImage.setImageURI(Uri.parse(product.imageUri));
                binding.productImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            } else {
                binding.productImage.setImageResource(R.drawable.ic_storefront);
                binding.productImage.setScaleType(
                        android.widget.ImageView.ScaleType.CENTER_INSIDE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onOpen(product));
        }
    }
}
