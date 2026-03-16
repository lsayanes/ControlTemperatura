package com.controltemperatura;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.controltemperatura.model.Paciente;
import com.controltemperatura.storage.CsvStorage;

import java.util.List;

/**
 * Recibe el Intent de la alarma y muestra la notificación de recordatorio.
 * Luego reprograma la siguiente alarma para mantener la repetición.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int pacienteId = intent.getIntExtra(NotificationHelper.EXTRA_PACIENTE_ID, -1);
        if (pacienteId == -1) return;

        // Buscar el nombre del paciente y su intervalo de alarma
        CsvStorage storage = new CsvStorage(context);
        List<Paciente> lista = storage.cargarPacientes();
        for (Paciente p : lista) {
            if (p.getId() == pacienteId) {
                // Mostrar la notificación
                NotificationHelper.mostrarRecordatorio(context, pacienteId, p.getNombreCompleto());

                // Reprogramar la siguiente alarma si sigue configurada
                if (p.getAlarmaHoras() > 0) {
                    AlarmScheduler.programar(context, p);
                }
                return;
            }
        }
    }
}
