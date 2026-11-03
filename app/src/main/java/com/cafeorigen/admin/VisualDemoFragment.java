package com.cafeorigen.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.databinding.FragmentVisualDemoBinding;

/**
 * CafeOrigen — Demo del sistema visual para roles pendientes (Comprador y
 * Vendedor se implementan en la fase siguiente).
 */
public class VisualDemoFragment extends Fragment {

    private FragmentVisualDemoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVisualDemoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.visual_demo_title));
        binding.demoChartView.setValues(1200f, 2100f, 1700f, 3400f, 2600f, 4200f, 3100f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
