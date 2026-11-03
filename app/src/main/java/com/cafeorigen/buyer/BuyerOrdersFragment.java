package com.cafeorigen.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.cafeorigen.databinding.FragmentBuyerOrdersBinding;

/**
 * CafeOrigen — Historial de compras del Comprador (C6).
 * Solo pedidos propios (buyerId), filtrables por estado.
 */
public class BuyerOrdersFragment extends Fragment {

    private BuyerOrdersViewModel viewModel;
    private FragmentBuyerOrdersBinding binding;
    private BuyerOrdersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(
                getString(R.string.buyer_orders_history_title));

        viewModel = new ViewModelProvider(requireActivity()).get(BuyerOrdersViewModel.class);
        adapter = new BuyerOrdersAdapter();
        binding.buyerOrdersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.buyerOrdersRecycler.setAdapter(adapter);

        binding.buyerOrdersFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            OrderStatus status = null;
            if (checked == R.id.chip_border_wait) {
                status = OrderStatus.POR_CONFIRMAR;
            } else if (checked == R.id.chip_border_confirmed) {
                status = OrderStatus.CONFIRMADO;
            } else if (checked == R.id.chip_border_dispatched) {
                status = OrderStatus.DESPACHADO;
            } else if (checked == R.id.chip_border_delivered) {
                status = OrderStatus.ENTREGADO;
            } else if (checked == R.id.chip_border_canceled) {
                status = OrderStatus.CANCELADO;
            }
            viewModel.setStatusFilter(status);
        });

        viewModel.getVisibleOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
            boolean empty = orders == null || orders.isEmpty();
            binding.buyerOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.buyerOrdersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.buyerOrdersEmpty.setTitle(getString(R.string.buyer_orders_empty_title));
                binding.buyerOrdersEmpty.setMessage(getString(R.string.buyer_orders_empty_msg));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.buyerOrdersProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.setStatusFilter(null);
        viewModel.load(SessionManager.get(requireContext()).getUserId());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca por si el vendedor avanzó el estado de algún pedido.
        if (viewModel != null) {
            viewModel.load(SessionManager.get(requireContext()).getUserId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
