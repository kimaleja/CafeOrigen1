package com.cafeorigen.vendor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.databinding.FragmentVendorOrdersBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Historial de pedidos recibidos (V6).
 * Chips por estado; el clic en fila abre el detalle (V7).
 */
public class VendorOrdersFragment extends Fragment implements VendorOrdersAdapter.Listener {

    private VendorOrdersViewModel viewModel;
    private FragmentVendorOrdersBinding binding;
    private VendorOrdersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.vendor_orders_title));

        // VM compartido con el detalle (V7) a nivel de Activity
        viewModel = new ViewModelProvider(requireActivity()).get(VendorOrdersViewModel.class);
        adapter = new VendorOrdersAdapter(this);

        binding.vendorOrdersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.vendorOrdersRecycler.setAdapter(adapter);

        // Filtro por estado
        binding.vendorOrdersFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            OrderStatus status = null;
            if (checked == R.id.chip_order_wait) {
                status = OrderStatus.POR_CONFIRMAR;
            } else if (checked == R.id.chip_order_confirmed) {
                status = OrderStatus.CONFIRMADO;
            } else if (checked == R.id.chip_order_dispatched) {
                status = OrderStatus.DESPACHADO;
            } else if (checked == R.id.chip_order_delivered) {
                status = OrderStatus.ENTREGADO;
            } else if (checked == R.id.chip_order_canceled) {
                status = OrderStatus.CANCELADO;
            }
            viewModel.setStatusFilter(status);
        });

        // Resultados de cambio de estado
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            if (state.status == VendorOrdersViewModel.OpsStatus.ERROR) {
                Toast.makeText(requireContext(), R.string.vendor_ops_error,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Listado + estado vacío
        viewModel.getVisibleOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
            boolean empty = orders == null || orders.isEmpty();
            binding.vendorOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.vendorOrdersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.vendorOrdersEmpty.setTitle(getString(R.string.vendor_orders_empty_title));
                binding.vendorOrdersEmpty.setMessage(getString(R.string.vendor_orders_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.vendorOrdersProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.setStatusFilter(null);

        // Mostrar spinner en la primera carga
        binding.vendorOrdersProgress.setVisibility(View.VISIBLE);
        viewModel.load(SessionManager.get(requireContext()).getUserId());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca tras cambiar estados en el detalle (V7).
        if (viewModel != null) {
            binding.vendorOrdersProgress.setVisibility(View.VISIBLE);
            viewModel.load(SessionManager.get(requireContext()).getUserId());
        }
    }

    @Override
    public void onOpen(Order order) {
        ((MainActivity) requireActivity()).openSub(
                VendorOrderDetailFragment.newInstance(order.id));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
