package com.controltemperatura;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.controltemperatura.model.Ficha;
import com.controltemperatura.model.Registro;
import com.controltemperatura.storage.CsvStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AgregarRegistroActivity extends AppCompatActivity {

    private static final SimpleDateFormat SDF_FECHA =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat SDF_HORA  =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private CsvStorage storage;
    private int pacienteId;

    // Calendario con la fecha/hora seleccionada (default: ahora)
    private final Calendar calendario = Calendar.getInstance();

    private TextInputEditText editFecha;
    private TextInputEditText editHora;
    private TextInputLayout   layoutTemperatura;
    private TextInputEditText editTemperatura;
    private TextInputEditText editDescripcion;
    private RadioGroup        radioGroupFicha;
    private RadioButton       radioFichaActual;
    private RadioButton       radioNuevaFicha;

    private Ficha fichaActiva;    // null si no hay ninguna activa
    private int   proximoNroFicha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_registro);

        pacienteId = getIntent().getIntExtra(PacienteDetalleActivity.EXTRA_PACIENTE_ID, -1);
        if (pacienteId == -1) { finish(); return; }

        storage = new CsvStorage(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        editFecha         = findViewById(R.id.editFecha);
        editHora          = findViewById(R.id.editHora);
        layoutTemperatura = findViewById(R.id.layoutTemperatura);
        editTemperatura   = findViewById(R.id.editTemperatura);
        editDescripcion   = findViewById(R.id.editDescripcion);
        radioGroupFicha   = findViewById(R.id.radioGroupFicha);
        radioFichaActual  = findViewById(R.id.radioFichaActual);
        radioNuevaFicha   = findViewById(R.id.radioNuevaFicha);

        // Fecha y hora: mostrar "ahora" por defecto
        actualizarCamposFechaHora();

        // Abrir DatePickerDialog al tocar fecha
        editFecha.setOnClickListener(v -> mostrarDatePicker());
        ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.layoutFecha))
                .setEndIconOnClickListener(v -> mostrarDatePicker());

        // Abrir TimePickerDialog al tocar hora
        editHora.setOnClickListener(v -> mostrarTimePicker());
        ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.layoutHora))
                .setEndIconOnClickListener(v -> mostrarTimePicker());

        // Configurar opciones de ficha
        configurarOpcionesFicha();

        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void actualizarCamposFechaHora() {
        editFecha.setText(SDF_FECHA.format(calendario.getTime()));
        editHora.setText(SDF_HORA.format(calendario.getTime()));
    }

    private void mostrarDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendario.set(Calendar.YEAR, year);
                    calendario.set(Calendar.MONTH, month);
                    calendario.set(Calendar.DAY_OF_MONTH, day);
                    actualizarCamposFechaHora();
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void mostrarTimePicker() {
        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    calendario.set(Calendar.HOUR_OF_DAY, hour);
                    calendario.set(Calendar.MINUTE, minute);
                    actualizarCamposFechaHora();
                },
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE),
                true // formato 24h
        ).show();
    }

    private void configurarOpcionesFicha() {
        fichaActiva = storage.getFichaActiva(pacienteId);

        List<Ficha> fichas = storage.cargarFichas(pacienteId);
        proximoNroFicha = fichas.isEmpty() ? 1
                : fichas.get(fichas.size() - 1).getNumero() + 1;

        if (fichaActiva != null) {
            // Hay ficha activa: ofrecer agregar a ella o crear nueva
            radioFichaActual.setText(getString(R.string.ficha_actual_label, fichaActiva.getNumero()));
            radioFichaActual.setChecked(true);
            radioNuevaFicha.setText(getString(R.string.nueva_ficha_label, proximoNroFicha));
        } else {
            // No hay ficha activa: solo se puede crear una nueva
            radioFichaActual.setVisibility(android.view.View.GONE);
            radioNuevaFicha.setText(getString(R.string.nueva_ficha_label, proximoNroFicha));
            radioNuevaFicha.setChecked(true);
        }
    }

    private void guardar() {
        // Validar temperatura
        String tempStr = editTemperatura.getText() != null
                ? editTemperatura.getText().toString().trim() : "";
        if (tempStr.isEmpty()) {
            layoutTemperatura.setError(getString(R.string.error_temp_obligatoria));
            editTemperatura.requestFocus();
            return;
        }
        layoutTemperatura.setError(null);

        float temperatura;
        try {
            temperatura = Float.parseFloat(tempStr.replace(',', '.'));
        } catch (NumberFormatException e) {
            layoutTemperatura.setError(getString(R.string.error_temp_invalida));
            return;
        }

        String descripcion = editDescripcion.getText() != null
                ? editDescripcion.getText().toString().trim() : "";

        long fechaHora = calendario.getTimeInMillis();

        // Determinar nro de ficha
        int nroFicha;
        boolean esNuevaFicha = radioNuevaFicha.isChecked();

        if (esNuevaFicha) {
            nroFicha = storage.crearNuevaFicha(pacienteId);
        } else {
            nroFicha = fichaActiva.getNumero();
        }

        Registro registro = new Registro(nroFicha, fechaHora, temperatura, descripcion);
        storage.agregarRegistro(pacienteId, registro);

        Toast.makeText(this, R.string.registro_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
