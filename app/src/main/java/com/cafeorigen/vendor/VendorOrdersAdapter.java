package com.cafeorigen.vendor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.databinding.ItemVendorOrderBinding;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CafeOrigen — Adaptador del historial de pedidos del Vendedor (V6).
 * Muestra comprador, resumen de artículos, antigüedad y estado; el click
 * abre el detalle (V7) manejado por el Fragment.
 */
public class VendorOrdersAdapter extends ListAdapter<Order, VendorOrdersAdapter.Holder> {

    /** Aviso de apertura de detalle a cargo del Fragment. */
    public interface Listener {
        void onOpen(Order order);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return java.util.Objects.equals(oldItem.status, newItem.status)
                    && java.util.Objects.equals(oldItem.buyerName, newItem.buyerName);
        }
    };

    public VendorOrdersAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVendorOrderBinding binding = ItemVendorOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemVendorOrderBinding binding;

        Holder(ItemVendorOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Order order) {
            Context context = binding.getRoot().getContext();
            NumberFormat cop = NumberFormat.getIntegerInstance(new Locale("es", "CO"));

            binding.orderBuyerText.setText(order.buyerName);
            binding.orderPriceText.setText("$" + cop.format(order.total));

            int units = 0;
            StringBuilder summary = new StringBuilder();
            for (int index = 0; index < order.items.size() && index < 3; index++) {
                OrderItem item = order.items.get(index);
                units += item.units;
                if (index > 0) {
                    summary.append(" · ");
                }
                summary.append(item.productName);
            }
            if (order.items.size() > 3) {
                summary.append(context.getString(R.string.vendor_order_more_fmt,
                        order.items.size() - 3));
            }
            binding.orderSummaryText.setText(summary.toString());
            binding.orderMetaText.setText(context.getString(
                    R.string.vendor_order_meta_fmt, order.daysAgo, units));

            // Badge de estado con paleta semafórica
            int bg;
            int tint;
            switch (order.status) {
                case POR_CONFIRMAR:
                    bg = R.drawable.bg_tag_warning;
                    tint = R.color.warning;
                    break;
                case CONFIRMADO:
                    bg = R.drawable.bg_tag_info;
                    tint = R.color.info;
                    break;
                case DESPACHADO:
                    bg = R.drawable.bg_tag_success;
                    tint = R.color.success;
                    break;
                case CANCELADO:
                    bg = R.drawable.bg_tag_danger;
                    tint = R.color.danger;
                    break;
                default:
                    bg = R.drawable.bg_tag_success;
                    tint = R.color.success;
                    break;
            }
            binding.orderStatusBadge.setBackgroundResource(bg);
            binding.orderStatusBadge.setTextColor(ContextCompat.getColor(context, tint));
            binding.orderStatusBadge.setText(order.status.getLabel());

            binding.getRoot().setOnClickListener(v -> listener.onOpen(order));
        }
    }
}
