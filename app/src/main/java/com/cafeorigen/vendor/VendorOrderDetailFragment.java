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
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderItem;
import com.cafeorigen.core.model.OrderStatus;
import com.cafeorigen.databinding.FragmentVendorOrderDetailBinding;
import com.cafeorigen.databinding.ItemVendorOrderLineBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Detalle de pedido del Vendedor (V7).
 * Muestra comprador, dirección, artículos y total; habilita las acciones
 * válidas según el estado (transiciones POR_CONFIRMAR → CONFIRMADO →
 * DESPACHADO → ENTREGADO, o CANCELADO desde POR_CONFIRMAR).
 */
public class VendorOrderDetailFragment extends Fragment {

    private static final String ARG_ORDER_ID = "orderId";

    private FragmentVendorOrderDetailBinding binding;
    private VendorOrdersViewModel viewModel;
    private String orderId;

    /** Fábrica del detalle (V7). */
    public static VendorOrderDetailFragment newInstance(String orderId) {
        VendorOrderDetailFragment fragment = new VendorOrderDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(
                getString(R.string.vendor_order_detail_title));

        viewModel = new ViewModelProvider(requireActivity()).get(VendorOrdersViewModel.class);

        if (getArguments() != null) {
            orderId = getArguments().getString(ARG_ORDER_ID, null);
        }

        binding.orderDetailBack.setOnClickListener(v -> popBack());

        binding.orderDetailPrimaryButton.setOnClickListener(v -> onPrimary());
        binding.orderDetailSecondaryButton.setOnClickListener(v -> onSecondary());

        // Resultados de cambio de estado (snack + refresco de la vista)
        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case CONFIRMED:
                    snack(getString(R.string.vendor_order_confirmed_fmt, orderId));
                    break;
                case DISPATCHED:
                    snack(getString(R.string.vendor_order_dispatched_fmt, orderId));
                    break;
                case DELIVERED:
                    snack(getString(R.string.vendor_order_delivered_fmt, orderId));
                    break;
                case CANCELED:
                    snack(getString(R.string.vendor_order_canceled_fmt, orderId));
                    break;
                case ERROR:
                    snack(getString(R.string.vendor_ops_error));
                    break;
                default:
                    break;
            }
            refresh(currentOrder());
        });

        refresh(currentOrder());
    }

    /** Orden actual del mock con verificación de propiedad. */
    private Order currentOrder() {
        if (orderId == null) {
            return null;
        }
        String vendorId = SessionManager.get(requireContext()).getUserId();
        Order order = MockDataProvider.findOrderById(orderId);
        // Permiso: solo visible si el pedido pertenece al vendedor en sesión.
        return order != null && order.vendorId.equals(vendorId) ? order : null;
    }

    private void refresh(Order order) {
        if (order == null || binding == null) {
            snack(getString(R.string.vendor_order_invalid));
            popBack();
            return;
        }

        binding.orderDetailBuyerText.setText(order.buyerName);
        binding.orderDetailPlacedText.setText(
                getString(R.string.vendor_order_placed_fmt, order.daysAgo));

        // Dirección
        binding.orderDetailAddressRow.setIcon(R.drawable.ic_location);
        binding.orderDetailAddressRow.setContent(
                getString(R.string.vendor_order_address_label), order.address);
        binding.orderDetailAddressRow.showChevron(false);

        // Estado con paleta semafórica
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
        binding.orderDetailStatusBadge.setBackgroundResource(bg);
        binding.orderDetailStatusBadge.setTextColor(
                ContextCompat.getColor(requireContext(), tint));
        binding.orderDetailStatusBadge.setText(order.status.getLabel());

        // Artículos
        binding.orderDetailItemsContainer.removeAllViews();
        for (OrderItem item : order.items) {
            ItemVendorOrderLineBinding line = ItemVendorOrderLineBinding.inflate(
                    getLayoutInflater(), binding.orderDetailItemsContainer, false);
            line.orderLineNameText.setText(item.productName);
            line.orderLineUnitsText.setText(
                    getString(R.string.vendor_line_units_fmt, item.units));
            line.orderLineSubtotalText.setText(com.cafeorigen.admin.MoneyFormat.cop(item.subtotal));
            binding.orderDetailItemsContainer.addView(line.getRoot());
        }

        binding.orderDetailTotalText.setText(com.cafeorigen.admin.MoneyFormat.cop(order.total));

        // Acciones según estado
        configureActions(order);
    }

    private void configureActions(Order order) {
        boolean primaryVisible = true;
        boolean secondaryVisible = false;
        boolean noteVisible = false;
        int primaryText = R.string.vendor_btn_confirm;

        switch (order.status) {
            case POR_CONFIRMAR:
                primaryText = R.string.vendor_btn_confirm;
                secondaryVisible = true;
                break;
            case CONFIRMADO:
                primaryText = R.string.vendor_btn_dispatch;
                break;
            case DESPACHADO:
                primaryText = R.string.vendor_btn_deliver;
                break;
            default:
                primaryVisible = false;
                noteVisible = true;
                break;
        }

        binding.orderDetailPrimaryButton.setVisibility(primaryVisible ? View.VISIBLE : View.GONE);
        binding.orderDetailSecondaryButton.setVisibility(secondaryVisible
                ? View.VISIBLE : View.GONE);
        binding.orderDetailNoteText.setVisibility(noteVisible ? View.VISIBLE : View.GONE);
        if (primaryVisible) {
            binding.orderDetailPrimaryButton.setText(primaryText);
        }
        if (secondaryVisible) {
            binding.orderDetailSecondaryButton.setText(R.string.vendor_btn_reject);
        }
    }

    private void onPrimary() {
        Order order = currentOrder();
        if (order == null) {
            return;
        }
        switch (order.status) {
            case POR_CONFIRMAR:
                viewModel.changeStatus(order, OrderStatus.CONFIRMADO);
                break;
            case CONFIRMADO:
                viewModel.changeStatus(order, OrderStatus.DESPACHADO);
                break;
            case DESPACHADO:
                viewModel.changeStatus(order, OrderStatus.ENTREGADO);
                break;
            default:
                break;
        }
    }

    private void onSecondary() {
        Order order = currentOrder();
        if (order == null || order.status != OrderStatus.POR_CONFIRMAR) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vendor_reject_title)
                .setMessage(getString(R.string.vendor_reject_msg_fmt,
                        String.valueOf(order.buyerName)))
                .setPositiveButton(R.string.vendor_btn_reject, (dialog, which) ->
                        viewModel.changeStatus(order, OrderStatus.CANCELADO))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void snack(String message) {
        if (binding != null && getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void popBack() {
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
