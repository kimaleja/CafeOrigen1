package com.cafeorigen.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.model.ProductStatus;
import com.cafeorigen.databinding.ItemAdminProductBinding;

/**
 * CafeOrigen — Adaptador de productos para moderación (A6).
 * Muestra solo las acciones aplicables según el estado del café.
 */
public class AdminProductsAdapter extends ListAdapter<Product, AdminProductsAdapter.Holder> {

    /** Eventos por fila manejados por el Fragment. */
    public interface Listener {
        void onApprove(Product product);

        void onReject(Product product);

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
            return java.util.Objects.equals(oldItem.status, newItem.status)
                    && java.util.Objects.equals(oldItem.name, newItem.name)
                    && oldItem.stock == newItem.stock && oldItem.price == newItem.price;
        }
    };

    public AdminProductsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminProductBinding binding = ItemAdminProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemAdminProductBinding binding;

        Holder(ItemAdminProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            Context context = binding.getRoot().getContext();

            binding.productNameText.setText(product.name);
            binding.productVendorText.setText(product.vendorName);
            binding.productMetaText.setText(context.getString(R.string.product_meta_fmt,
                    product.region, product.variety, product.altitudeM));
            binding.productPriceText.setText(MoneyFormat.cop(product.price));

            applyStatus(context, product);

            boolean moderable = product.status == ProductStatus.PENDIENTE;
            binding.productApproveButton.setVisibility(moderable ? View.VISIBLE : View.GONE);
            binding.productRejectButton.setVisibility(moderable ? View.VISIBLE : View.GONE);
            if (moderable) {
                binding.productApproveButton.setOnClickListener(v -> listener.onApprove(product));
                binding.productRejectButton.setOnClickListener(v -> listener.onReject(product));
            }

            binding.productDeleteButton.setOnClickListener(v -> listener.onDelete(product));
        }

        private void applyStatus(Context context, Product product) {
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
        }
    }
}
