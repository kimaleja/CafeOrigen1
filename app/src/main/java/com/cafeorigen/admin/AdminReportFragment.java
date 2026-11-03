package com.cafeorigen.admin;

import android.content.Intent;
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
import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.databinding.FragmentAdminReportBinding;

/**
 * CafeOrigen — Reporte de ventas (A7).
 * Periodo (7/30/365 días), KPIs, gráfico, ranking y exportación CSV.
 */
public class AdminReportFragment extends Fragment {

    private AdminReportViewModel viewModel;
    private FragmentAdminReportBinding binding;
    private ReportAdapter adapter;
    private AdminDataRepository.ReportData lastReport; // para CSV

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.admin_report_title));

        viewModel = new ViewModelProvider(this).get(AdminReportViewModel.class);
        adapter = new ReportAdapter();
        binding.reportRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reportRecycler.setAdapter(adapter);

        // Cambio de periodo
        binding.reportPeriodChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked == R.id.chip_report_7) {
                viewModel.setPeriod(AdminReportViewModel.PERIOD_7);
            } else if (checked == R.id.chip_report_365) {
                viewModel.setPeriod(AdminReportViewModel.PERIOD_365);
            } else {
                viewModel.setPeriod(AdminReportViewModel.PERIOD_30);
            }
        });

        binding.reportExportButton.setOnClickListener(v -> exportCsv());

        viewModel.getReport().observe(getViewLifecycleOwner(), data -> {
            lastReport = data;
            boolean empty = data == null || data.rows.isEmpty();
            binding.reportEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.reportContentGroup.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.reportEmptyState.setTitle(getString(R.string.report_empty_title));
                binding.reportEmptyState.setMessage(getString(R.string.report_empty_msg));
                return;
            }
            binding.reportKpiRevenue.setValue(MoneyFormat.cop(data.totalRevenue));
            binding.reportKpiUnits.setValue(String.valueOf(data.totalUnits));
            binding.reportChartView.setValues(data.dailyChart);
            adapter.setRows(data.rows);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.reportProgress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));

        // Selecciona el chip de 30 días y dispara la primera carga
        binding.chipReport30.setChecked(true);
    }

    /** Exporta el CSV del reporte con confirmación y comparte por chooser. */
    private void exportCsv() {
        if (lastReport == null) {
            return;
        }
        String csv = AdminDataRepository.get().buildCsv(lastReport);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_csv_title));
        share.putExtra(Intent.EXTRA_TEXT, csv);
        startActivity(Intent.createChooser(share, getString(R.string.share_csv_title)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.load();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
