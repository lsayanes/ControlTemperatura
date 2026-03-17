package com.pyrexlog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.pyrexlog.model.Ficha;
import com.pyrexlog.model.Paciente;
import com.pyrexlog.storage.CsvStorage;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CurvaActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID  = "paciente_id";
    public static final String EXTRA_FICHA_NUMERO = "ficha_numero"; // -1 = última

    private static final String FILE_PROVIDER_AUTH = "com.pyrexlog.fileprovider";

    private static final SimpleDateFormat SDF_FICHA =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private CsvStorage storage;
    private List<Ficha> fichas;
    private int fichaIndex;
    private Paciente paciente;

    private TempChartView chartView;
    private TextView      textFichaNavLabel;
    private TextView      textSinDatos;
    private ImageButton   btnAnterior;
    private ImageButton   btnSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curva);

        int pacienteId = getIntent().getIntExtra(EXTRA_PACIENTE_ID, -1);
        if (pacienteId == -1) { finish(); return; }

        storage  = new CsvStorage(this);
        fichas   = storage.cargarFichas(pacienteId);
        paciente = buscarPaciente(pacienteId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (paciente != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(paciente.getNombreCompleto());
            getSupportActionBar().setSubtitle(R.string.ver_curva);
        }

        chartView         = findViewById(R.id.chartView);
        textFichaNavLabel = findViewById(R.id.textFichaNavLabel);
        textSinDatos      = findViewById(R.id.textSinDatos);
        btnAnterior       = findViewById(R.id.btnFichaAnterior);
        btnSiguiente      = findViewById(R.id.btnFichaSiguiente);

        btnAnterior.setOnClickListener(v -> navegarFicha(-1));
        btnSiguiente.setOnClickListener(v -> navegarFicha(+1));
        findViewById(R.id.fabCompartir).setOnClickListener(v -> compartirGrafico());

        if (fichas.isEmpty()) {
            mostrarSinDatos();
            return;
        }

        int fichaNumero = getIntent().getIntExtra(EXTRA_FICHA_NUMERO, -1);
        fichaIndex = fichas.size() - 1;
        if (fichaNumero != -1) {
            for (int i = 0; i < fichas.size(); i++) {
                if (fichas.get(i).getNumero() == fichaNumero) {
                    fichaIndex = i;
                    break;
                }
            }
        }

        mostrarFicha(fichaIndex);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void navegarFicha(int delta) {
        int nuevo = fichaIndex + delta;
        if (nuevo >= 0 && nuevo < fichas.size()) {
            fichaIndex = nuevo;
            mostrarFicha(fichaIndex);
        }
    }

    private void mostrarFicha(int index) {
        Ficha ficha = fichas.get(index);

        String estado = ficha.isCerrada()
                ? getString(R.string.cerrada) : getString(R.string.abierta);
        textFichaNavLabel.setText(getString(R.string.nav_ficha_label,
                ficha.getNumero(),
                SDF_FICHA.format(new Date(ficha.getFechaInicio())),
                estado));

        btnAnterior.setEnabled(index > 0);
        btnAnterior.setAlpha(index > 0 ? 1f : 0.3f);
        btnSiguiente.setEnabled(index < fichas.size() - 1);
        btnSiguiente.setAlpha(index < fichas.size() - 1 ? 1f : 0.3f);

        if (ficha.getRegistros().isEmpty()) {
            chartView.setVisibility(View.GONE);
            textSinDatos.setVisibility(View.VISIBLE);
        } else {
            textSinDatos.setVisibility(View.GONE);
            chartView.setVisibility(View.VISIBLE);
            chartView.setRegistros(ficha.getRegistros());
        }
    }

    private void mostrarSinDatos() {
        chartView.setVisibility(View.GONE);
        textSinDatos.setVisibility(View.VISIBLE);
        textFichaNavLabel.setText(R.string.sin_fichas);
        btnAnterior.setEnabled(false);
        btnSiguiente.setEnabled(false);
    }

    // ── Compartir ─────────────────────────────────────────────────────────────

    private void compartirGrafico() {
        if (fichas.isEmpty() || fichas.get(fichaIndex).getRegistros().isEmpty()) {
            Toast.makeText(this, R.string.ficha_sin_registros, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = renderizarGraficoCompleto();
            Uri uri = guardarBitmapEnCache(bitmap);
            lanzarIntentCompartir(uri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_compartir, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Dibuja el gráfico completo (incluyendo un encabezado con nombre y ficha)
     * en un Bitmap fuera de pantalla. Captura el ancho total del chartView
     * aunque esté dentro de un HorizontalScrollView.
     */
    private Bitmap renderizarGraficoCompleto() {
        Ficha ficha = fichas.get(fichaIndex);

        // Asegurar que el chartView esté medido con su ancho completo
        int chartW = chartView.getMeasuredWidth();
        int chartH = chartView.getMeasuredHeight();
        if (chartW == 0 || chartH == 0) {
            // Forzar medición si aún no ocurrió
            int specW = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int specH = View.MeasureSpec.makeMeasureSpec(chartH > 0 ? chartH : 600,
                    View.MeasureSpec.EXACTLY);
            chartView.measure(specW, specH);
            chartView.layout(0, 0, chartView.getMeasuredWidth(), chartView.getMeasuredHeight());
            chartW = chartView.getMeasuredWidth();
            chartH = chartView.getMeasuredHeight();
        }

        float density   = getResources().getDisplayMetrics().density;
        int   headerH   = (int) (48 * density);
        int   totalH    = headerH + chartH;

        Bitmap bitmap = Bitmap.createBitmap(chartW, totalH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // Encabezado: nombre del paciente + ficha
        Paint paintHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHeader.setColor(Color.parseColor("#424242"));
        paintHeader.setTextSize(14 * density);
        paintHeader.setTextAlign(Paint.Align.LEFT);
        paintHeader.setFakeBoldText(true);

        String titulo = (paciente != null ? paciente.getNombreCompleto() : "")
                + "  —  " + getString(R.string.ficha) + " " + ficha.getNumero()
                + "  (" + SDF_FICHA.format(new Date(ficha.getFechaInicio())) + ")";
        canvas.drawText(titulo, 12 * density, headerH * 0.65f, paintHeader);

        // Gráfico debajo del encabezado
        canvas.save();
        canvas.translate(0, headerH);
        chartView.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    /** Guarda el bitmap como PNG en el directorio de caché y devuelve su Uri. */
    private Uri guardarBitmapEnCache(Bitmap bitmap) throws IOException {
        File dir = new File(getCacheDir(), "compartir");
        if (!dir.exists()) dir.mkdirs();

        String nombre = "curva_" + System.currentTimeMillis() + ".png";
        File archivo = new File(dir, nombre);

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
        }

        return FileProvider.getUriForFile(this, FILE_PROVIDER_AUTH, archivo);
    }

    /** Lanza el selector de apps para compartir la imagen. */
    private void lanzarIntentCompartir(Uri uri) {
        Ficha ficha = fichas.get(fichaIndex);
        String texto = getString(R.string.compartir_texto,
                paciente != null ? paciente.getNombreCompleto() : "",
                ficha.getNumero());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, texto);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.compartir)));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Paciente buscarPaciente(int id) {
        for (Paciente p : storage.cargarPacientes()) {
            if (p.getId() == id) return p;
        }
        return null;
    }
}
