package com.cafeorigen.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.cafeorigen.R;
import com.cafeorigen.databinding.ViewRowItemBinding;

/**
 * CafeOrigen — Componente reutilizable: fila de ajuste/entrada de lista
 * (direcciones, ajustes de perfil, cierre de sesión, alertas…).
 * Atributos XML: rowIcon, rowTitle, rowSubtitle, rowShowChevron.
 * Métodos programáticos para reutilización en listas dinámicas.
 */
public class RowItemView extends android.widget.LinearLayout {

    private final ViewRowItemBinding binding;

    public RowItemView(Context context) {
        this(context, null);
    }

    public RowItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RowItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        setFocusable(true);
        // La raíz del layout es esta misma clase, por eso se infla sobre self.
        inflate(context, R.layout.view_row_item, this);
        binding = ViewRowItemBinding.bind(this);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RowItem, 0, 0);
            try {
                int iconRes = a.getResourceId(R.styleable.RowItem_rowIcon, 0);
                String title = a.getString(R.styleable.RowItem_rowTitle);
                String subtitle = a.getString(R.styleable.RowItem_rowSubtitle);
                boolean chevron = a.getBoolean(R.styleable.RowItem_rowShowChevron, true);

                if (iconRes != 0) {
                    binding.rowIconImage.setImageResource(iconRes);
                }
                if (title != null) {
                    binding.rowTitleText.setText(title);
                }
                if (subtitle != null) {
                    binding.rowSubtitleText.setText(subtitle);
                }
                binding.rowChevronImage.setVisibility(chevron ? View.VISIBLE : View.GONE);
            } finally {
                a.recycle();
            }
        }
    }

    /** Cambia el ícono de la fila (uso programático en listas/alertas). */
    public void setIcon(int iconRes) {
        binding.rowIconImage.setImageResource(iconRes);
    }

    /** Tinta personalizada del ícono (paleta de estados). */
    public void setIconColor(int color) {
        binding.rowIconImage.setColorFilter(color);
        binding.rowIconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    /** Textos título y subtítulo programáticos. */
    public void setContent(CharSequence title, CharSequence subtitle) {
        binding.rowTitleText.setText(title);
        if (subtitle == null || subtitle.length() == 0) {
            binding.rowSubtitleText.setVisibility(View.GONE);
        } else {
            binding.rowSubtitleText.setVisibility(View.VISIBLE);
            binding.rowSubtitleText.setText(subtitle);
        }
    }

    /** Muestra u oculta el chevron final. */
    public void showChevron(boolean visible) {
        binding.rowChevronImage.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public View getRowRoot() {
        return this;
    }
}
