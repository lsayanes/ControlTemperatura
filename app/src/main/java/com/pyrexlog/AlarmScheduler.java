package com.pyrexlog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.pyrexlog.model.Paciente;

/**
 * Programa y cancela alarmas de recordatorio usando AlarmManager.
 *
 * Cada paciente tiene su propia alarma identificada por su id.
 * Se usa setExactAndAllowWhileIdle para que funcione en modo Doze (ahorro de batería).
 */
public class AlarmScheduler {

    /**
     * Programa (o reprograma) la próxima alarma para el paciente.
     * Se dispara en alarmaHoras horas a partir de ahora.
     */
    public static void programar(Context context, Paciente paciente) {
        if (paciente.getAlarmaHoras() <= 0) return;

        long intervaloMs = paciente.getAlarmaHoras() * 60L * 60L * 1000L;
        long disparoMs   = System.currentTimeMillis() + intervaloMs;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(context, paciente.getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparoMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, disparoMs, pi);
        }
    }

    /**
     * Cancela la alarma del paciente (cuando se quita el recordatorio o se elimina).
     */
    public static void cancelar(Context context, int pacienteId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildPendingIntent(context, pacienteId));
    }

    private static PendingIntent buildPendingIntent(Context context, int pacienteId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(NotificationHelper.EXTRA_PACIENTE_ID, pacienteId);
        return PendingIntent.getBroadcast(
                context, pacienteId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
