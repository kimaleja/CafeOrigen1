package com.cafeorigen.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.cafeorigen.R;

/**
 * CafeOrigen — Componente propio: gráfico de barras animado por código,
 * construido sin librerías externas para los reportes/dashboard del sistema.
 * Atributos: barColor (barras), barTrackColor (pista de fondo).
 */
public class BarChartView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] values = new float[0];
    private int barColor = Color.rgb(200, 161, 90);   // @color/accent_gold por defecto
    private int trackColor = Color.rgb(239, 235, 233); // @color/cafe_100 por defecto

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BarChart, 0, 0);
            try {
                barColor = a.getColor(R.styleable.BarChart_barColor, barColor);
                trackColor = a.getColor(R.styleable.BarChart_barTrackColor, trackColor);
            } finally {
                a.recycle();
            }
        }
        trackPaint.setStyle(Paint.Style.FILL);
        barPaint.setStyle(Paint.Style.FILL);
    }

    /** Asigna los valores del gráfico y redibuja. */
    public void setValues(float... newValueList) {
        values = newValueList == null ? new float[0] : newValueList.clone();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int barCount = values.length;
        if (barCount == 0) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float gap = width * 0.02f;
        float slot = width / barCount;
        float barWidth = slot - gap * 2;
        if (barWidth <= 0) {
            barWidth = slot * 0.6f;
        }
        float corner = barWidth / 2f;

        float max = 0f;
        for (float v : values) {
            max = Math.max(max, v);
        }
        if (max <= 0) {
            max = 1f;
        }

        for (int index = 0; index < barCount; index++) {
            float left = index * slot + gap;
            float trackTop = 0f;
            float trackBottom = height;

            // Pista redondeada de fondo
            trackPaint.setColor(trackColor);
            canvas.drawRoundRect(new RectF(left, trackTop, left + barWidth, trackBottom), corner, corner, trackPaint);

            // Barra proporcional al valor (mínimo 6% para evitar barras invisibles)
            float fraction = values[index] / max;
            float barHeight = (height - 4f) * Math.max(fraction, 0.06f);
            float top = height - barHeight;

            barPaint.setColor(barColor);
            canvas.drawRoundRect(new RectF(left, top, left + barWidth, height), corner, corner, barPaint);
        }
    }
}
