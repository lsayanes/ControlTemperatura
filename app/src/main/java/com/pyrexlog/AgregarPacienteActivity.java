package com.pyrexlog;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pyrexlog.model.Paciente;
import com.pyrexlog.storage.CsvStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class AgregarPacienteActivity extends AppCompatActivity {

    // Intervalos de alarma disponibles (horas). 0 = sin alarma.
    private static final int[] ALARMA_VALORES = {0, 2, 4, 6, 8, 12, 24};

    private CsvStorage storage;
    private int pacienteId = 0; // 0 = modo nuevo

    private TextInputLayout layoutNombre;
    private TextInputEditText editNombre;
    private TextInputEditText editApellido;
    private TextInputEditText editEdad;
    private RadioGroup radioGroupSexo;
    private TextInputEditText editDescripcion;
    private Spinner spinnerAlarma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_paciente);

        storage = new CsvStorage(this);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas del formulario
        layoutNombre   = findViewById(R.id.layoutNombre);
        editNombre     = findViewById(R.id.editNombre);
        editApellido   = findViewById(R.id.editApellido);
        editEdad       = findViewById(R.id.editEdad);
        radioGroupSexo = findViewById(R.id.radioGroupSexo);
        editDescripcion = findViewById(R.id.editDescripcion);
        spinnerAlarma  = findViewById(R.id.spinnerAlarma);

        configurarSpinnerAlarma();

        // ¿Modo edición?
        if (getIntent().hasExtra(MainActivity.EXTRA_PACIENTE_ID)) {
            pacienteId = getIntent().getIntExtra(MainActivity.EXTRA_PACIENTE_ID, 0);
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.editar_paciente);
            cargarDatosPaciente(pacienteId);
        } else {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.nuevo_paciente);
        }

        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // El botón "Eliminar" solo aparece en modo edición
        if (pacienteId != 0) {
            getMenuInflater().inflate(R.menu.menu_editar_paciente, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_eliminar) {
            confirmarEliminar();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void configurarSpinnerAlarma() {
        String[] etiquetas = getResources().getStringArray(R.array.alarma_opciones);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, etiquetas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarma.setAdapter(adapter);
    }

    private void cargarDatosPaciente(int id) {
        List<Paciente> lista = storage.cargarPacientes();
        for (Paciente p : lista) {
            if (p.getId() == id) {
                editNombre.setText(p.getNombre());
                editApellido.setText(p.getApellido());
                if (p.getEdad() >= 0)
                    editEdad.setText(String.valueOf(p.getEdad()));
                editDescripcion.setText(p.getDescripcion());

                switch (p.getSexo()) {
                    case "M": radioGroupSexo.check(R.id.radioMasculino);    break;
                    case "F": radioGroupSexo.check(R.id.radioFemenino);     break;
                    default:  radioGroupSexo.check(R.id.radioSinEspecificar); break;
                }

                seleccionarAlarma(p.getAlarmaHoras());
                return;
            }
        }
        Toast.makeText(this, R.string.error_cargar_paciente, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void seleccionarAlarma(int horas) {
        for (int i = 0; i < ALARMA_VALORES.length; i++) {
            if (ALARMA_VALORES[i] == horas) {
                spinnerAlarma.setSelection(i);
                return;
            }
        }
        spinnerAlarma.setSelection(0); // sin alarma
    }

    private void guardar() {
        String nombre = editNombre.getText() != null
                ? editNombre.getText().toString().trim() : "";

        if (nombre.isEmpty()) {
            layoutNombre.setError(getString(R.string.error_nombre_obligatorio));
            editNombre.requestFocus();
            return;
        }
        layoutNombre.setError(null);

        String apellido = editApellido.getText() != null
                ? editApellido.getText().toString().trim() : "";
        String edadStr  = editEdad.getText() != null
                ? editEdad.getText().toString().trim() : "";
        int edad = edadStr.isEmpty() ? -1 : Integer.parseInt(edadStr);
        String descripcion = editDescripcion.getText() != null
                ? editDescripcion.getText().toString().trim() : "";

        String sexo;
        int radioId = radioGroupSexo.getCheckedRadioButtonId();
        if (radioId == R.id.radioMasculino)       sexo = "M";
        else if (radioId == R.id.radioFemenino)   sexo = "F";
        else                                       sexo = "";

        int alarmaHoras = ALARMA_VALORES[spinnerAlarma.getSelectedItemPosition()];

        Paciente paciente = new Paciente(
                pacienteId, nombre, apellido, edad, sexo, descripcion,
                System.currentTimeMillis(), alarmaHoras);

        storage.guardarPaciente(paciente);

        // Programar o cancelar alarma según el intervalo elegido
        if (alarmaHoras > 0) {
            AlarmScheduler.programar(this, paciente);
        } else {
            AlarmScheduler.cancelar(this, paciente.getId());
        }

        Toast.makeText(this, R.string.paciente_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_paciente)
                .setMessage(R.string.confirmar_eliminar_paciente)
                .setPositiveButton(R.string.eliminar_paciente, (d, w) -> {
                    AlarmScheduler.cancelar(this, pacienteId);
                    storage.eliminarPaciente(pacienteId);
                    Toast.makeText(this, R.string.paciente_eliminado, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
