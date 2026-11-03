package com.cafeorigen.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.ui.RowItemView;
import com.cafeorigen.databinding.FragmentBuyerProfileBinding;

/**
 * CafeOrigen — Perfil del Comprador (C6).
 * Datos de la cuenta, accesos al historial/carrito y cierre de sesión.
 */
public class BuyerProfileFragment extends Fragment {

    private FragmentBuyerProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.vendor_profile_title));

        SessionManager session = SessionManager.get(requireContext());
        String fullName = session.getUserName();
        String name = fullName == null ? "" : (fullName.indexOf('·') > 0
                ? fullName.substring(0, fullName.indexOf('·')).trim() : fullName.trim());
        String initials = com.cafeorigen.admin.MoneyFormat.initials(
                name.isEmpty() ? "·" : name);

        binding.buyerProfileAvatar.setText(initials);
        binding.buyerProfileNameText.setText(name);
        binding.buyerProfileEmailText.setText(session.getUserEmail());

        RowItemView historyRow = binding.buyerProfileHistoryRow;
        historyRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootOrders());
        binding.buyerProfileCartRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRootCart());
        binding.buyerProfileLogoutRow.setOnClickListener(v ->
                ((MainActivity) requireActivity()).requestLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca el nombre por si se actualizó en otra sesión de la app.
        SessionManager session = SessionManager.get(requireContext());
        String fullName = session.getUserName();
        String name = fullName == null ? "" : (fullName.indexOf('·') > 0
                ? fullName.substring(0, fullName.indexOf('·')).trim() : fullName.trim());
        binding.buyerProfileNameText.setText(name);
        binding.buyerProfileAvatar.setText(
                com.cafeorigen.admin.MoneyFormat.initials(name.isEmpty() ? "·" : name));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
