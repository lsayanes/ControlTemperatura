package com.pyrexlog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyrexlog.model.Paciente;
import com.pyrexlog.storage.CsvStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "paciente_id";

    private CsvStorage storage;
    private PacienteAdapter adapter;
    private List<Paciente> listaPacientes;
    private TextView textSinPacientes;

    // Launcher para solicitar permiso de notificaciones (Android 13+)
    private final ActivityResultLauncher<String> permNotifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // El usuario aceptó o rechazó; el canal ya está creado de todas formas
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new CsvStorage(this);

        // Crear canal de notificaciones (requerido en Android 8+)
        NotificationHelper.crearCanal(this);

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textSinPacientes = findViewById(R.id.textSinPacientes);

        listaPacientes = new ArrayList<>();
        adapter = new PacienteAdapter(listaPacientes, new PacienteAdapter.OnPacienteClickListener() {
            @Override
            public void onPacienteClick(Paciente paciente) {
                onPacienteSeleccionado(paciente);
            }
            @Override
            public void onPacienteLongClick(Paciente paciente) {
                abrirEditarPaciente(paciente);
            }
        });

        RecyclerView recycler = findViewById(R.id.recyclerPacientes);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNuevoPaciente);
        fab.setOnClickListener(v -> abrirAgregarPaciente());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPacientes();
    }

    private void cargarPacientes() {
        listaPacientes.clear();
        listaPacientes.addAll(storage.cargarPacientes());
        adapter.notifyDataSetChanged();
        textSinPacientes.setVisibility(listaPacientes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onPacienteSeleccionado(Paciente paciente) {
        // Actualizar último acceso
        paciente.setUltimoAcceso(System.currentTimeMillis());
        storage.guardarPaciente(paciente);

        Intent intent = new Intent(this, PacienteDetalleActivity.class);
        intent.putExtra(PacienteDetalleActivity.EXTRA_PACIENTE_ID, paciente.getId());
        startActivity(intent);
    }

    private void abrirAgregarPaciente() {
        startActivity(new Intent(this, AgregarPacienteActivity.class));
    }

    private void abrirEditarPaciente(Paciente paciente) {
        Intent intent = new Intent(this, AgregarPacienteActivity.class);
        intent.putExtra(EXTRA_PACIENTE_ID, paciente.getId());
        startActivity(intent);
    }
}
