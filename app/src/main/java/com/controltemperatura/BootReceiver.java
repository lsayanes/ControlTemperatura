package com.controltemperatura;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.controltemperatura.model.Paciente;
import com.controltemperatura.storage.CsvStorage;

/**
 * Reprograma todas las alarmas activas después de un reinicio del dispositivo.
 * Las alarmas del AlarmManager se pierden cuando el teléfono se apaga.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        CsvStorage storage = new CsvStorage(context);
        for (Paciente p : storage.cargarPacientes()) {
            if (p.getAlarmaHoras() > 0) {
                AlarmScheduler.programar(context, p);
            }
        }
    }
}
