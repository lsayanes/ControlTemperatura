package com.pyrexlog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

/**
 * Crea el canal de notificación y muestra notificaciones de recordatorio.
 */
public class NotificationHelper {

    static final String CHANNEL_ID   = "recordatorio_temperatura";
    static final String EXTRA_PACIENTE_ID = "paciente_id";

    /** Crea el canal (idempotente; se puede llamar varias veces sin problema). */
    public static void crearCanal(Context context) {
        NotificationChannel canal = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_canal_nombre),
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(context.getString(R.string.notif_canal_desc));
        context.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    /** Muestra la notificación de recordatorio para el paciente indicado. */
    public static void mostrarRecordatorio(Context context, int pacienteId, String nombrePaciente) {
        // Al tocar la notificación → abrir PacienteDetalleActivity
        Intent intent = new Intent(context, PacienteDetalleActivity.class);
        intent.putExtra(PacienteDetalleActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, pacienteId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notif_titulo))
                .setContentText(context.getString(R.string.notif_texto, nombrePaciente))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        context.getSystemService(NotificationManager.class)
                .notify(pacienteId, builder.build());
    }
}
