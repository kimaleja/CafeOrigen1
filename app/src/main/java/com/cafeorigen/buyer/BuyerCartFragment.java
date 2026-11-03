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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.admin.MoneyFormat;
import com.cafeorigen.databinding.FragmentBuyerCartBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Carrito de compras del Comprador (C4).
 * Cantidades (+/−), quitar líneas, total recalculado y paso a la pasarela (C5).
 */
public class BuyerCartFragment extends Fragment implements BuyerCartAdapter.Listener {

    private BuyerCartViewModel viewModel;
    private FragmentBuyerCartBinding binding;
    private BuyerCartAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.buyer_cart_title));

        viewModel = new ViewModelProvider(this).get(BuyerCartViewModel.class);
        adapter = new BuyerCartAdapter(this);
        binding.buyerCartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.buyerCartRecycler.setAdapter(adapter);

        binding.buyerCartPayButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSub(new BuyerCheckoutFragment()));

        // Total en vivo
        viewModel.getLines().observe(getViewLifecycleOwner(), lines -> {
            adapter.submitList(lines);
            refreshFooter(lines);
        });
    }

    private void refreshFooter(java.util.List<BuyerCartViewModel.CartLine> lines) {
        boolean empty = lines == null || lines.isEmpty();
        binding.buyerCartEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.buyerCartFooter.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.buyerCartRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            binding.buyerCartEmpty.setTitle(getString(R.string.buyer_cart_empty_title));
            binding.buyerCartEmpty.setMessage(getString(R.string.buyer_cart_empty_msg));
        }
        // Total recalculado con los precios actuales del mock
        long total = viewModel.total();
        binding.buyerCartTotalText.setText(MoneyFormat.cop(total));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    // =========== Acciones del Adapter.Listener (C4) ===========

    @Override
    public void onIncrease(BuyerCartViewModel.CartLine line) {
        if (!viewModel.updateUnits(line.product.id, line.units + 1)) {
            Toast.makeText(requireContext(), R.string.buyer_cart_qty_max,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDecrease(BuyerCartViewModel.CartLine line) {
        if (line.units > 1) {
            viewModel.updateUnits(line.product.id, line.units - 1);
        } else {
            confirmRemove(line);
        }
    }

    @Override
    public void onRemove(BuyerCartViewModel.CartLine line) {
        confirmRemove(line);
    }

    private void confirmRemove(BuyerCartViewModel.CartLine line) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.buyer_cart_remove_cd)
                .setMessage(getString(R.string.buyer_delete_line_msg_fmt,
                        String.valueOf(line.product.name)))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.remove(line.product.id);
                    Snackbar.make(binding.getRoot(),
                            getString(R.string.buyer_cart_removed_fmt,
                                    String.valueOf(line.product.name)),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
