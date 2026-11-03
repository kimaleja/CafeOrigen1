package com.cafeorigen.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.cafeorigen.R;
import com.cafeorigen.databinding.ViewKpiCardBinding;

/**
 * CafeOrigen — Componente reutilizable: tarjeta KPI.
 * Usada en dashboards de los tres roles (ventas, usuarios, stock…).
 * Atributos XML (res/values/attrs.xml): kpiValue, kpiLabel, kpiIcon, kpiAccentColor.
 */
public class KpiCardView extends FrameLayout {

    private final ViewKpiCardBinding binding;

    public KpiCardView(Context context) {
        this(context, null);
    }

    public KpiCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KpiCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // El layout raíz es <merge>; se infla directamente sobre este contenedor.
        LayoutInflater.from(context).inflate(R.layout.view_kpi_card, this, true);
        binding = ViewKpiCardBinding.bind(this);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.KpiCard, 0, 0);
            try {
                String value = a.getString(R.styleable.KpiCard_kpiValue);
                String label = a.getString(R.styleable.KpiCard_kpiLabel);
                int iconRes = a.getResourceId(R.styleable.KpiCard_kpiIcon, 0);
                int accent = a.getColor(R.styleable.KpiCard_kpiAccentColor, 0);

                if (value != null) {
                    binding.kpiValueText.setText(value);
                }
                if (label != null) {
                    binding.kpiLabelText.setText(label);
                }
                if (iconRes != 0) {
                    binding.kpiIconImage.setImageResource(iconRes);
                }
                if (accent != 0) {
                    setAccent(accent);
                }
            } finally {
                a.recycle();
            }
        }
    }

    /** Acento parametrizable por pantalla (color del ícono y la burbuja). */
    public void setAccent(int color) {
        binding.kpiIconImage.setColorFilter(color);
        binding.kpiIconContainer.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /** Actualiza el valor del KPI por código (ViewModels). */
    public void setValue(CharSequence value) {
        binding.kpiValueText.setText(value);
    }
}
