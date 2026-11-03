package com.cafeorigen.core.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.cafeorigen.R;
import com.cafeorigen.databinding.ViewEmptyStateBinding;

/**
 * CafeOrigen — Componente reutilizable: estado vacío de listas/catálogos.
 * Los mensajes por pantalla se personalizan con setTitle/setMessage.
 */
public class EmptyStateView extends android.widget.LinearLayout {

    private final ViewEmptyStateBinding binding;

    public EmptyStateView(Context context) {
        this(context, null);
    }

    public EmptyStateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_empty_state, this);
        binding = ViewEmptyStateBinding.bind(this);
    }

    /** Personaliza el título por pantalla. */
    public void setTitle(CharSequence title) {
        binding.emptyTitleText.setText(title);
    }

    /** Personaliza el mensaje descriptivo por pantalla. */
    public void setMessage(CharSequence message) {
        binding.emptyMessageText.setText(message);
    }
}
