package com.cafeorigen.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.util.Validators;
import com.cafeorigen.databinding.FragmentBuyerCheckoutBinding;
import com.cafeorigen.databinding.ItemVendorOrderLineBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Pasarela de pagos del Comprador (C5).
 * Resumen de compra, formulario de entrega y métodos de pago solo demo:
 * no existe proveedor de pagos real y NO se solicitan datos bancarios.
 */
public class BuyerCheckoutFragment extends Fragment {

    private FragmentBuyerCheckoutBinding binding;
    private BuyerCheckoutViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.buyer_checkout_title));

        viewModel = new ViewModelProvider(this).get(BuyerCheckoutViewModel.class);

        bindSummary();
        setupActions();
        observeOps();
    }

    /** Resumen de la compra con líneas y total del carrito actual (C5). */
    private void bindSummary() {
        java.util.List<CartRepository.CartItem> items = CartRepository.get().items();
        long total = CartRepository.get().total();
        binding.checkoutSummaryContainer.removeAllViews();

        boolean empty = items.isEmpty();
        binding.checkoutConfirmButton.setEnabled(!empty);
        binding.checkoutTotalText.setText(MoneyFormat.cop(total));
        if (empty) {
            Snackbar.make(binding.getRoot(), R.string.buyer_checkout_empty_msg,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        for (CartRepository.CartItem item : items) {
            com.cafeorigen.core.model.Product product =
                    com.cafeorigen.core.data.MockDataProvider.findProductById(item.productId);
            if (product == null) {
                continue;
            }
            ItemVendorOrderLineBinding line = ItemVendorOrderLineBinding.inflate(
                    getLayoutInflater(), binding.checkoutSummaryContainer, false);
            line.orderLineNameText.setText(product.name);
            line.orderLineUnitsText.setText(
                    getString(R.string.vendor_line_units_fmt, item.units));
            line.orderLineSubtotalText.setText(MoneyFormat.cop(
                    (long) product.price * item.units));
            binding.checkoutSummaryContainer.addView(line.getRoot());
        }
        binding.checkoutConfirmButton.setText(getString(
                R.string.buyer_confirm_order_fmt, MoneyFormat.cop(total)));
    }

    private void setupActions() {
        binding.checkoutBackRow.setOnClickListener(v -> popBack());
        binding.checkoutConfirmButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        if (!validateDelivery()) {
            return;
        }
        BuyerCheckoutViewModel.PaymentMethod method = selectedMethod();
        BuyerCheckoutViewModel.DeliveryDraft draft = new BuyerCheckoutViewModel.DeliveryDraft(
                String.valueOf(binding.checkoutNameEdit.getText()).trim(),
                String.valueOf(binding.checkoutPhoneEdit.getText()).trim(),
                String.valueOf(binding.checkoutAddressEdit.getText()).trim(),
                method);

        SessionManager session = SessionManager.get(requireContext());
        viewModel.checkout(draft, session.getUserId(),
                session.getUserName() == null ? draft.recipientName : session.getUserName());
    }

    private BuyerCheckoutViewModel.PaymentMethod selectedMethod() {
        int checked = binding.checkoutPaymentRadios.getCheckedRadioButtonId();
        if (checked == R.id.radio_payment_pse) {
            return BuyerCheckoutViewModel.PaymentMethod.PSE_DEMO;
        }
        if (checked == R.id.radio_payment_cash) {
            return BuyerCheckoutViewModel.PaymentMethod.CASH_DEMO;
        }
        return BuyerCheckoutViewModel.PaymentMethod.CARD_DEMO;
    }

    private boolean validateDelivery() {
        boolean valid = true;
        binding.checkoutNameInput.setError(null);
        binding.checkoutAddressInput.setError(null);
        binding.checkoutPhoneInput.setError(null);

        if (!Validators.notEmpty(String.valueOf(binding.checkoutNameEdit.getText()).trim())) {
            binding.checkoutNameInput.setError(getString(R.string.buyer_error_recipient));
            valid = false;
        }
        String phone = String.valueOf(binding.checkoutPhoneEdit.getText()).trim();
        if (phone.replaceAll("\\D", "").length() < 7) {
            binding.checkoutPhoneInput.setError(getString(R.string.vendor_error_phone));
            valid = false;
        }
        if (!Validators.notEmpty(String.valueOf(binding.checkoutAddressEdit.getText()).trim())) {
            binding.checkoutAddressInput.setError(getString(R.string.buyer_error_address));
            valid = false;
        }
        return valid;
    }

    private void observeOps() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.checkoutProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        viewModel.getOpsState().observe(getViewLifecycleOwner(), state -> {
            if (state == null
                    || state.status == BuyerCheckoutViewModel.OpsStatus.IDLE) {
                return;
            }
            // Se marca entregado para no repetirlo tras rotar (consumeOps).
            viewModel.consumeOps();
            switch (state.status) {
                case SUCCESS:
                    showSuccess(state);
                    break;
                case STOCK_CHANGED:
                    Toast.makeText(requireContext(), R.string.buyer_stock_changed,
                            Toast.LENGTH_LONG).show();
                    popBack();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), R.string.buyer_checkout_error,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });
    }

    /** Confirmación visual del pedido (sin pago real). */
    private void showSuccess(BuyerCheckoutViewModel.OpsState state) {
        if (state.createdOrders == null || state.createdOrders.isEmpty()) {
            return;
        }
        StringBuilder ids = new StringBuilder();
        long total = 0;
        for (com.cafeorigen.core.model.Order order : state.createdOrders) {
            if (ids.length() > 0) {
                ids.append(", ");
            }
            ids.append(order.id);
            total += order.total;
        }

        binding.checkoutContainer.setVisibility(View.GONE);
        binding.checkoutSuccessContainer.setVisibility(View.VISIBLE);
        binding.checkoutSuccessMessageText.setText(
                getString(R.string.buyer_success_message_fmt, MoneyFormat.cop(total),
                        state.createdOrders.get(0).buyerName));
        binding.checkoutSuccessOrdersText.setText(
                getString(R.string.buyer_success_orders_fmt, ids.toString()));

        binding.checkoutSuccessOrdersButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootOrders());
        binding.checkoutSuccessKeepButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootCatalog(null));
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
