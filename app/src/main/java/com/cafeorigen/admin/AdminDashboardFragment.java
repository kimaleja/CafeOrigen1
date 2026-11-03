package com.cafeorigen.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.ui.RowItemView;
import com.cafeorigen.databinding.FragmentAdminDashboardBinding;

/**
 * CafeOrigen — Dashboard del Administrador (A1).
 * KPIs del mes, gráfico de ventas, alertas operativas y accesos rápidos
 * hacia Usuarios, Productos y Reporte de ventas.
 */
public class AdminDashboardFragment extends Fragment {

    private AdminDashboardViewModel viewModel;
    private FragmentAdminDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.admin_dashboard_title));

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);

        String name = SessionManager.get(requireContext()).getUserName();
        binding.dashGreetingText.setText(getString(R.string.dash_greeting_fmt,
                name == null ? getString(R.string.role_label_admin) : name
                        .split("·")[0].trim()));

        // Accesos rápidos
        binding.dashRowUsers.setOnClickListener(v -> openUsers());
        binding.dashRowProducts.setOnClickListener(v -> openProducts());
        binding.dashRowReport.setOnClickListener(v -> openReport());

        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) {
                return;
            }
            bindStats(stats);
        });

        viewModel.load();
    }

    private void bindStats(AdminDataRepository.DashboardStats stats) {
        binding.dashKpiRevenue.setValue(revenueLabel(stats.revenueMonth));
        binding.dashKpiUsers.setValue(String.valueOf(stats.usersCount));
        binding.dashKpiProducts.setValue(String.valueOf(stats.productsCount));

        binding.dashChartView.setValues(stats.weeklyChart);
        binding.dashChartLabelsText.setText(weekLabels());

        renderAlerts(stats);
    }

    private String revenueLabel(long amount) {
        if (amount >= 1_000_000) {
            return getString(R.string.dash_value_millions_fmt, amount / 1_000_000f);
        }
        return MoneyFormat.cop(amount);
    }

    private String weekLabels() {
        // Etiquetas cortas de la semana: L M X J V S D y orden cronológico
        java.time.DayOfWeek[] days = new java.time.DayOfWeek[7];
        for (int index = 0; index < 7; index++) {
            days[index] = java.time.LocalDate.now().minusDays(6 - index).getDayOfWeek();
        }
        String[] labels = {"D", "L", "M", "X", "J", "V", "S"};
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            label.append(labels[days[i].getValue() % 7]).append("  ");
        }
        return label.toString().trim();
    }

    /** Alertas como filas del sistema visual con íconos coherentes. */
    private void renderAlerts(AdminDataRepository.DashboardStats stats) {
        binding.dashAlertsContainer.removeAllViews();
        boolean hasAlerts = false;

        if (stats.pendingModeration > 0) {
            binding.dashAlertsContainer.addView(buildAlert(R.drawable.ic_assignment,
                    R.string.dash_alert_pending_fmt, stats.pendingModeration, R.color.warning));
            hasAlerts = true;
        }
        if (!stats.lowStock.isEmpty()) {
            binding.dashAlertsContainer.addView(buildAlert(R.drawable.ic_warning,
                    R.string.dash_alert_lowstock_fmt, stats.lowStock.size(), R.color.warning));
            hasAlerts = true;
        }
        if (stats.usersCount > 0 && SessionManager.get(requireContext()) != null) {
            // Presencia de cuentas desactivadas (borrado lógico) por revisar
            binding.dashAlertsContainer.addView(buildAlert(R.drawable.ic_group,
                    R.string.dash_alert_inactive_fmt, 0, R.color.info));
            hasAlerts = true;
        }

        binding.dashAlertsEmpty.setVisibility(hasAlerts ? View.GONE : View.VISIBLE);
        binding.dashAlertsContainer.setVisibility(hasAlerts ? View.VISIBLE : View.GONE);
        if (!hasAlerts) {
            binding.dashAlertsEmpty.setTitle(getString(R.string.dash_no_alerts));
        }
    }

    private View buildAlert(int iconRes, int textRes, int value, int colorRes) {
        RowItemView row = new RowItemView(requireContext());
        row.setIcon(iconRes);
        row.setContent(getString(textRes, value), null);
        row.showChevron(true);
        row.setIconColor(ContextCompat.getColor(requireContext(), colorRes));
        row.setOnClickListener(v -> {
            if (textRes == R.string.dash_alert_lowstock_fmt || textRes == R.string.dash_alert_pending_fmt) {
                openProducts();
            } else {
                openUsers();
            }
        });
        return row;
    }

    private void openUsers() {
        ((MainActivity) requireActivity()).openSub(new AdminUserListFragment());
    }

    private void openProducts() {
        ((MainActivity) requireActivity()).openSub(new AdminProductListFragment());
    }

    private void openReport() {
        ((MainActivity) requireActivity()).openSub(new AdminReportFragment());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
