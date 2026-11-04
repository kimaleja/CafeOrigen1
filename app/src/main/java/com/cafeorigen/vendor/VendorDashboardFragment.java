package com.cafeorigen.vendor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.data.VendorDataRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.Product;
import com.cafeorigen.core.ui.RowItemView;
import com.cafeorigen.databinding.FragmentVendorDashboardBinding;

/**
 * CafeOrigen — Panel principal del Vendedor (V1).
 * KPIs, gráfico semanal, pedidos por confirmar y alertas de stock;
 * todas las cifras provienen del mock filtrado por vendorId.
 */
public class VendorDashboardFragment extends Fragment {

    private VendorDashboardViewModel viewModel;
    private FragmentVendorDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.vendor_dashboard_title));

        viewModel = new ViewModelProvider(this).get(VendorDashboardViewModel.class);

        String fullName = SessionManager.get(requireContext()).getUserName();
        binding.vendorGreetingText.setText(getString(R.string.dash_greeting_fmt, greetName(fullName)));

        viewModel.getStats().observe(getViewLifecycleOwner(), this::bindStats);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load(SessionManager.get(requireContext()).getUserId());
    }

    private String greetName(String fullName) {
        if (fullName == null) {
            return getString(R.string.role_label_vendor);
        }
        int split = fullName.indexOf('·');
        return (split > 0 ? fullName.substring(0, split).trim() : fullName.trim());
    }

    private void bindStats(VendorDataRepository.VendorDashboard stats) {
        // El LiveData entrega null inicial (antes de la respuesta del mock);
        // y el binding puede estar nulo si la vista se destruyó durante la latencia.
        if (stats == null || binding == null) {
            return;
        }
        binding.vendorKpiSales.setValue(revenueLabel(stats.salesMonth));
        binding.vendorKpiActive.setValue(String.valueOf(stats.activeProducts));
        binding.vendorKpiPending.setValue(String.valueOf(stats.pendingOrders));

        binding.vendorChartView.setValues(stats.weeklyChart);

        renderOrders(stats);
        renderStock(stats);
    }

    private String revenueLabel(long amount) {
        if (amount >= 1_000_000) {
            return getString(R.string.dash_value_millions_fmt, amount / 1_000_000f);
        }
        return com.cafeorigen.admin.MoneyFormat.cop(amount);
    }

    /** Pedidos por confirmar (clic → detalle V7). */
    private void renderOrders(VendorDataRepository.VendorDashboard stats) {
        binding.vendorOrdersContainer.removeAllViews();
        boolean hasOrders = !stats.toConfirm.isEmpty();
        binding.vendorOrdersEmpty.setVisibility(hasOrders ? View.GONE : View.VISIBLE);
        binding.vendorOrdersContainer.setVisibility(hasOrders ? View.VISIBLE : View.GONE);
        if (!hasOrders) {
            binding.vendorOrdersEmpty.setTitle(getString(R.string.vendor_orders_empty_title));
            binding.vendorOrdersEmpty.setMessage(getString(R.string.vendor_orders_empty_msg));
            return;
        }
        for (Order order : stats.toConfirm) {
            RowItemView row = new RowItemView(requireContext());
            row.setIcon(R.drawable.ic_receipt);
            row.setContent(order.buyerName,
                    getString(R.string.vendor_order_meta_fmt, order.daysAgo,
                            totalUnits(order)));
            row.showChevron(true);
            row.setIconColor(ContextCompat.getColor(requireContext(), R.color.warning));
            row.setOnClickListener(v -> ((MainActivity) requireActivity()).openSub(
                    VendorOrderDetailFragment.newInstance(order.id)));
            binding.vendorOrdersContainer.addView(row);
        }
    }

    /** Stock bajo de productos activos (clic → productos V2). */
    private void renderStock(VendorDataRepository.VendorDashboard stats) {
        binding.vendorStockContainer.removeAllViews();
        boolean hasLowStock = !stats.lowStock.isEmpty();
        binding.vendorStockEmpty.setVisibility(hasLowStock ? View.GONE : View.VISIBLE);
        binding.vendorStockContainer.setVisibility(hasLowStock ? View.VISIBLE : View.GONE);
        if (!hasLowStock) {
            binding.vendorStockEmpty.setTitle(getString(R.string.vendor_stock_empty_title));
            binding.vendorStockEmpty.setMessage(getString(R.string.vendor_stock_empty_msg));
            return;
        }
        for (Product product : stats.lowStock) {
            RowItemView row = new RowItemView(requireContext());
            row.setIcon(R.drawable.ic_warning);
            row.setContent(product.name,
                    getString(R.string.vendor_stock_fmt, product.stock));
            row.showChevron(true);
            row.setIconColor(ContextCompat.getColor(requireContext(), R.color.warning));
            row.setOnClickListener(v ->
                    ((MainActivity) requireActivity()).openRootProducts());
            binding.vendorStockContainer.addView(row);
        }
    }

    private int totalUnits(Order order) {
        int units = 0;
        for (int index = 0; index < order.items.size(); index++) {
            units += order.items.get(index).units;
        }
        return units;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
