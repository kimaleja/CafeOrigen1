package com.cafeorigen.vendor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.databinding.ItemVendorProductBinding;

/**
 * CafeOrigen — Adaptador del listado de productos del Vendedor (V2).
 * Imagen (uri de galería o fábrica), precio, stock y menú de acciones.
 */
public class VendorProductsAdapter extends ListAdapter<Product, VendorProductsAdapter.Holder> {

    /** Eventos de fila manejados por el Fragment. */
    public interface Listener {
        void onEdit(Product product);

        void onDelete(Product product);
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
                    && java.util.Objects.equals(oldItem.status, newItem.status)
                    && java.util.Objects.equals(oldItem.category, newItem.category)
                    && java.util.Objects.equals(oldItem.imageUri, newItem.imageUri);
        }
    };

    public VendorProductsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVendorProductBinding binding = ItemVendorProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemVendorProductBinding binding;

        Holder(ItemVendorProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            Context context = binding.getRoot().getContext();
            binding.productNameText.setText(product.name);
            binding.productPriceText.setText(MoneyFormat.cop(product.price));
            binding.productStockText.setText(context.getString(
                    R.string.vendor_stock_fmt, product.stock));
            binding.productCategoryText.setText(product.category == null
                    ? "" : product.category.getLabel());

            // Imagen: uri de galería (V3) o ícono de fábrica
            if (product.imageUri != null && !product.imageUri.isEmpty()) {
                binding.productImage.setImageURI(Uri.parse(product.imageUri));
            } else {
                binding.productImage.setImageResource(R.drawable.ic_storefront);
            }
            binding.productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.productImage.setBackgroundColor(ContextCompat.getColor(
                    context, R.color.cafe_100));

            // Badge de estado
            int bg;
            int tint;
            switch (product.status) {
                case PENDIENTE:
                    bg = R.drawable.bg_tag_warning;
                    tint = R.color.warning;
                    break;
                case ACTIVO:
                    bg = R.drawable.bg_tag_success;
                    tint = R.color.success;
                    break;
                case RECHAZADO:
                    bg = R.drawable.bg_tag_danger;
                    tint = R.color.danger;
                    break;
                default:
                    bg = R.drawable.bg_tag_info;
                    tint = R.color.info;
                    break;
            }
            binding.productStatusBadge.setBackgroundResource(bg);
            binding.productStatusBadge.setTextColor(ContextCompat.getColor(context, tint));
            binding.productStatusBadge.setText(product.status.getLabel());

            // Menú contextual por ítem (editar / eliminar)
            binding.productOverflowButton.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(context, v);
                menu.getMenuInflater().inflate(R.menu.menu_overflow_product, menu.getMenu());
                menu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_product_edit) {
                        listener.onEdit(product);
                        return true;
                    }
                    if (id == R.id.action_product_delete) {
                        listener.onDelete(product);
                        return true;
                    }
                    return false;
                });
                menu.show();
            });
        }
    }
}
