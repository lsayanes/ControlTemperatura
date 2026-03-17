package com.pyrexlog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyrexlog.model.Ficha;
import com.pyrexlog.model.Paciente;
import com.pyrexlog.storage.CsvStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class PacienteDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "paciente_id";

    private CsvStorage storage;
    private int pacienteId;
    private FichaAdapter adapter;
    private TextView textSinRegistros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_detalle);

        pacienteId = getIntent().getIntExtra(EXTRA_PACIENTE_ID, -1);
        if (pacienteId == -1) { finish(); return; }

        storage = new CsvStorage(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nombre del paciente en el toolbar
        Paciente paciente = buscarPaciente(pacienteId);
        if (paciente != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(paciente.getNombreCompleto());
        }

        textSinRegistros = findViewById(R.id.textSinRegistros);
        adapter = new FichaAdapter();

        RecyclerView recycler = findViewById(R.id.recyclerFichas);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAgregarRegistro);
        fab.setOnClickListener(v -> abrirAgregarRegistro());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarFichas();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paciente_detalle, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ver_curva) {
            abrirCurva();
            return true;
        } else if (id == R.id.action_editar_paciente) {
            Intent intent = new Intent(this, AgregarPacienteActivity.class);
            intent.putExtra(MainActivity.EXTRA_PACIENTE_ID, pacienteId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_nueva_ficha) {
            confirmarNuevaFicha();
            return true;
        } else if (id == R.id.action_cerrar_ficha) {
            confirmarCerrarFicha();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void cargarFichas() {
        List<Ficha> fichas = storage.cargarFichas(pacienteId);
        adapter.cargarFichas(fichas);
        textSinRegistros.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirAgregarRegistro() {
        Intent intent = new Intent(this, AgregarRegistroActivity.class);
        intent.putExtra(EXTRA_PACIENTE_ID, pacienteId);
        startActivity(intent);
    }

    private void abrirCurva() {
        Intent intent = new Intent(this, CurvaActivity.class);
        intent.putExtra(CurvaActivity.EXTRA_PACIENTE_ID, pacienteId);
        startActivity(intent);
    }

    private void confirmarNuevaFicha() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.nueva_ficha)
                .setMessage(R.string.confirmar_nueva_ficha)
                .setPositiveButton(R.string.crear, (d, w) -> {
                    int nro = storage.crearNuevaFicha(pacienteId);
                    Toast.makeText(this,
                            getString(R.string.ficha_creada, nro),
                            Toast.LENGTH_SHORT).show();
                    cargarFichas();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void confirmarCerrarFicha() {
        Ficha activa = storage.getFichaActiva(pacienteId);
        if (activa == null) {
            Toast.makeText(this, R.string.no_hay_ficha_activa, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.cerrar_ficha)
                .setMessage(getString(R.string.confirmar_cerrar_ficha, activa.getNumero()))
                .setPositiveButton(R.string.cerrar_ficha, (d, w) -> {
                    storage.cerrarFicha(pacienteId, activa.getNumero());
                    Toast.makeText(this, R.string.ficha_cerrada, Toast.LENGTH_SHORT).show();
                    cargarFichas();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private Paciente buscarPaciente(int id) {
        for (Paciente p : storage.cargarPacientes()) {
            if (p.getId() == id) return p;
        }
        return null;
    }
}
