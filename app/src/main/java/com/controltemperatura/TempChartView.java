package com.controltemperatura;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.controltemperatura.model.Registro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Vista personalizada que dibuja un gráfico de barras de temperaturas.
 * Se coloca dentro de un HorizontalScrollView para soportar muchos registros.
 *
 * Colores según especificación:
 *   Verde:   temperatura ≤ 37.0°C
 *   Naranja: 37.1°C – 37.4°C
 *   Rojo:    ≥ 37.5°C
 */
public class TempChartView extends View {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final int COLOR_VERDE   = Color.parseColor("#4CAF50");
    private static final int COLOR_NARANJA = Color.parseColor("#FF9800");
    private static final int COLOR_ROJO    = Color.parseColor("#F44336");
    private static final int COLOR_GRID    = Color.parseColor("#E0E0E0");
    private static final int COLOR_LINEA37 = Color.parseColor("#BDBDBD");
    private static final int COLOR_TEXTO   = Color.parseColor("#424242");

    // ── Dimensiones (dp → se convierten en onCreate vía densidad) ────────────
    private float BAR_WIDTH;
    private float BAR_GAP;
    private float PADDING_LEFT;
    private float PADDING_RIGHT;
    private float PADDING_TOP;
    private float PADDING_BOTTOM;   // espacio para etiquetas X
    private float LABEL_SIZE;
    private float VALUE_SIZE;
    private float AXIS_LABEL_SIZE;

    // ── Rango del eje Y ───────────────────────────────────────────────────────
    private static final float Y_MIN_DEFAULT = 35.5f;
    private static final float Y_MAX_DEFAULT = 40.5f;

    // ── Datos ─────────────────────────────────────────────────────────────────
    private List<Registro> registros = new ArrayList<>();
    private float yMin = Y_MIN_DEFAULT;
    private float yMax = Y_MAX_DEFAULT;

    // ── Paints ────────────────────────────────────────────────────────────────
    private Paint paintBarra;
    private Paint paintGrid;
    private Paint paintLinea37;
    private Paint paintTextoValor;
    private Paint paintTextoEjeX;
    private Paint paintTextoEjeY;

    private static final SimpleDateFormat SDF_LABEL =
            new SimpleDateFormat("dd/MM\nHH:mm", Locale.getDefault());

    public TempChartView(Context context) {
        super(context);
        init();
    }

    public TempChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float d = getContext().getResources().getDisplayMetrics().density;
        BAR_WIDTH       = 44 * d;
        BAR_GAP         = 12 * d;
        PADDING_LEFT    = 48 * d;
        PADDING_RIGHT   = 16 * d;
        PADDING_TOP     = 32 * d;
        PADDING_BOTTOM  = 56 * d;
        LABEL_SIZE      = 10 * d;
        VALUE_SIZE      = 11 * d;
        AXIS_LABEL_SIZE = 10 * d;

        paintBarra = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGrid.setColor(COLOR_GRID);
        paintGrid.setStrokeWidth(1 * d);

        paintLinea37 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLinea37.setColor(COLOR_LINEA37);
        paintLinea37.setStrokeWidth(1.5f * d);
        paintLinea37.setPathEffect(new DashPathEffect(new float[]{8 * d, 4 * d}, 0));

        paintTextoValor = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextoValor.setColor(COLOR_TEXTO);
        paintTextoValor.setTextSize(VALUE_SIZE);
        paintTextoValor.setTextAlign(Paint.Align.CENTER);

        paintTextoEjeX = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextoEjeX.setColor(COLOR_TEXTO);
        paintTextoEjeX.setTextSize(LABEL_SIZE);
        paintTextoEjeX.setTextAlign(Paint.Align.CENTER);

        paintTextoEjeY = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextoEjeY.setColor(COLOR_TEXTO);
        paintTextoEjeY.setTextSize(AXIS_LABEL_SIZE);
        paintTextoEjeY.setTextAlign(Paint.Align.RIGHT);
    }

    /** Carga los registros a graficar y redibuja. */
    public void setRegistros(List<Registro> registros) {
        this.registros = registros;
        // Calcular rango Y dinámico
        if (!registros.isEmpty()) {
            float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
            for (Registro r : registros) {
                if (r.getTemperatura() < min) min = r.getTemperatura();
                if (r.getTemperatura() > max) max = r.getTemperatura();
            }
            yMin = Math.min(Y_MIN_DEFAULT, (float) Math.floor(min) - 0.5f);
            yMax = Math.max(Y_MAX_DEFAULT, (float) Math.ceil(max)  + 0.5f);
        }
        requestLayout();
        invalidate();
    }

    public List<Registro> getRegistros() {
        return registros;
    }

    // ── Medición ──────────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int n = Math.max(registros.size(), 1);
        int w = (int) (PADDING_LEFT + PADDING_RIGHT + n * (BAR_WIDTH + BAR_GAP));
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) h = (int) (200 * getContext().getResources().getDisplayMetrics().density);
        setMeasuredDimension(w, h);
    }

    // ── Dibujo ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (registros.isEmpty()) return;

        float chartTop    = PADDING_TOP;
        float chartBottom = getHeight() - PADDING_BOTTOM;
        float chartLeft   = PADDING_LEFT;
        float chartHeight = chartBottom - chartTop;

        // Líneas de grid horizontales (cada 0.5°C)
        float step = 0.5f;
        for (float temp = (float) Math.ceil(yMin / step) * step; temp <= yMax; temp += step) {
            float y = tempToY(temp, chartTop, chartHeight);
            canvas.drawLine(chartLeft, y, getWidth() - PADDING_RIGHT, y, paintGrid);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", temp),
                    chartLeft - 4, y + AXIS_LABEL_SIZE / 2, paintTextoEjeY);
        }

        // Línea de referencia a 37.0°C
        float y37 = tempToY(37.0f, chartTop, chartHeight);
        Path path37 = new Path();
        path37.moveTo(chartLeft, y37);
        path37.lineTo(getWidth() - PADDING_RIGHT, y37);
        canvas.drawPath(path37, paintLinea37);

        // Barras
        for (int i = 0; i < registros.size(); i++) {
            Registro r = registros.get(i);
            float barX = chartLeft + i * (BAR_WIDTH + BAR_GAP);
            float barTop = tempToY(r.getTemperatura(), chartTop, chartHeight);

            paintBarra.setColor(colorParaTemp(r.getTemperatura()));
            canvas.drawRect(barX, barTop, barX + BAR_WIDTH, chartBottom, paintBarra);

            // Valor encima de la barra
            String valLabel = String.format(Locale.getDefault(), "%.1f", r.getTemperatura());
            canvas.drawText(valLabel, barX + BAR_WIDTH / 2, barTop - 4, paintTextoValor);

            // Etiqueta de fecha/hora debajo del eje X (en dos líneas)
            String fechaLabel = SDF_LABEL.format(new Date(r.getFechaHora()));
            String[] partes = fechaLabel.split("\n");
            float labelCenterX = barX + BAR_WIDTH / 2;
            canvas.drawText(partes[0], labelCenterX, chartBottom + LABEL_SIZE + 4, paintTextoEjeX);
            if (partes.length > 1)
                canvas.drawText(partes[1], labelCenterX, chartBottom + LABEL_SIZE * 2 + 6, paintTextoEjeX);
        }
    }

    /** Convierte un valor de temperatura a coordenada Y en el canvas. */
    private float tempToY(float temp, float chartTop, float chartHeight) {
        float ratio = (yMax - temp) / (yMax - yMin);
        return chartTop + ratio * chartHeight;
    }

    /** Devuelve el color de barra según la temperatura. */
    private int colorParaTemp(float temp) {
        if (temp <= 37.0f)       return COLOR_VERDE;
        else if (temp <= 37.4f)  return COLOR_NARANJA;
        else                     return COLOR_ROJO;
    }
}
