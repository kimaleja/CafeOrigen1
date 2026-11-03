package com.cafeorigen.buyer;

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
import com.cafeorigen.databinding.ItemBuyerOrderBinding;

/**
 * CafeOrigen — Adaptador del historial de compras del Comprador (C6).
 * Filas de solo lectura con resumen, total, antigüedad y estado.
 */
public class BuyerOrdersAdapter extends ListAdapter<Order, BuyerOrdersAdapter.Holder> {

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return java.util.Objects.equals(oldItem.status, newItem.status);
        }
    };

    public BuyerOrdersAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuyerOrderBinding binding = ItemBuyerOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemBuyerOrderBinding binding;

        Holder(ItemBuyerOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Order order) {
            Context context = binding.getRoot().getContext();
            binding.orderIdText.setText(context.getString(
                    R.string.buyer_order_id_fmt, order.id));

            // Resumen: primer producto + resto
            int items = order.items.size();
            String summary = items > 0 ? order.items.get(0).productName : "—";
            if (items > 1) {
                summary += context.getString(R.string.vendor_order_more_fmt, items - 1);
            }
            binding.orderSummaryText.setText(summary);
            binding.orderMetaText.setText(context.getString(
                    R.string.vendor_order_meta_fmt, order.daysAgo, totalUnits(order)));
            binding.orderTotalText.setText(MoneyFormat.cop(order.total));

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
        }

        private int totalUnits(Order order) {
            int units = 0;
            for (int index = 0; index < order.items.size(); index++) {
                units += order.items.get(index).units;
            }
            return units;
        }
    }
}
