package com.cafeorigen.buyer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.databinding.ItemBuyerCartBinding;

/**
 * CafeOrigen — Adaptador del carrito de compras (C4).
 * Stepper de cantidades (+/−) y acción de quitar manejadas por el Fragment.
 */
public class BuyerCartAdapter extends ListAdapter<BuyerCartViewModel.CartLine,
        BuyerCartAdapter.Holder> {

    /** Eventos de la fila a cargo del Fragment/ViewModel. */
    public interface Listener {
        void onIncrease(BuyerCartViewModel.CartLine line);

        void onDecrease(BuyerCartViewModel.CartLine line);

        void onRemove(BuyerCartViewModel.CartLine line);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<BuyerCartViewModel.CartLine> DIFF =
            new DiffUtil.ItemCallback<BuyerCartViewModel.CartLine>() {
                @Override
                public boolean areItemsTheSame(@NonNull BuyerCartViewModel.CartLine oldItem,
                                               @NonNull BuyerCartViewModel.CartLine newItem) {
                    return oldItem.product.id.equals(newItem.product.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull BuyerCartViewModel.CartLine oldItem,
                                                  @NonNull BuyerCartViewModel.CartLine newItem) {
                    return oldItem.units == newItem.units
                            && oldItem.product.stock == newItem.product.stock;
                }
            };

    public BuyerCartAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuyerCartBinding binding = ItemBuyerCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemBuyerCartBinding binding;

        Holder(ItemBuyerCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BuyerCartViewModel.CartLine line) {
            Context context = binding.getRoot().getContext();
            binding.cartNameText.setText(line.product.name);
            binding.cartUnitPriceText.setText(
                    context.getString(R.string.buyer_cart_unit_price_fmt,
                            line.product.price));
            binding.cartQtyText.setText(String.valueOf(line.units));
            binding.cartSubtotalText.setText(MoneyFormat.cop(line.subtotal));

            // Imagen de galería o ícono de fábrica
            if (line.product.imageUri != null && !line.product.imageUri.isEmpty()) {
                binding.cartImage.setImageURI(Uri.parse(line.product.imageUri));
                binding.cartImage.setScaleType(
                        android.widget.ImageView.ScaleType.CENTER_CROP);
            } else {
                binding.cartImage.setImageResource(R.drawable.ic_storefront);
                binding.cartImage.setScaleType(
                        android.widget.ImageView.ScaleType.CENTER_INSIDE);
            }

            binding.cartMinusButton.setOnClickListener(v -> listener.onDecrease(line));
            binding.cartPlusButton.setOnClickListener(v -> listener.onIncrease(line));
            binding.cartRemoveButton.setOnClickListener(v -> listener.onRemove(line));
        }
    }
}
