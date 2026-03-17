package com.pyrexlog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyrexlog.model.Paciente;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.ViewHolder> {

    public interface OnPacienteClickListener {
        void onPacienteClick(Paciente paciente);
        void onPacienteLongClick(Paciente paciente);
    }

    private final List<Paciente> lista;
    private final OnPacienteClickListener listener;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public PacienteAdapter(List<Paciente> lista, OnPacienteClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paciente paciente = lista.get(position);
        holder.textNombre.setText(paciente.getNombreCompleto());
        holder.textAcceso.setText(SDF.format(new Date(paciente.getUltimoAcceso())));
        holder.itemView.setOnClickListener(v -> listener.onPacienteClick(paciente));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onPacienteLongClick(paciente);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void actualizar(List<Paciente> nuevaLista) {
        lista.clear();
        lista.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre;
        TextView textAcceso;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.textNombrePaciente);
            textAcceso = itemView.findViewById(R.id.textUltimoAcceso);
        }
    }
}
