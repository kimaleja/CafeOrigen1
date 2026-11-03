package com.cafeorigen.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.R;
import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.databinding.ItemReportRowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — Adaptador del ranking de productos del reporte (A7).
 * El listado llega del ViewModel del periodo seleccionado.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.Holder> {

    private List<AdminDataRepository.ReportRow> rows = new ArrayList<>();

    public void setRows(List<AdminDataRepository.ReportRow> newRows) {
        rows = newRows == null ? new ArrayList<>() : newRows;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportRowBinding binding = ItemReportRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AdminDataRepository.ReportRow row = rows.get(position);
        holder.binding.reportRankText.setText(
                holder.binding.getRoot().getContext().getString(R.string.report_rank_fmt, position + 1));
        holder.binding.reportProductNameText.setText(row.productName);
        holder.binding.reportProductUnitsText.setText(
                holder.binding.getRoot().getContext().getString(R.string.product_stock_fmt, row.units));
        holder.binding.reportProductRevenueText.setText(MoneyFormat.cop(row.revenue));
    }

    class Holder extends RecyclerView.ViewHolder {

        final ItemReportRowBinding binding;

        Holder(ItemReportRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
