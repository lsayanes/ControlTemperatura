package com.pyrexlog;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyrexlog.model.Ficha;
import com.pyrexlog.model.Registro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FichaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_FICHA   = 0;
    private static final int TIPO_REGISTRO = 1;

    // Color thresholds según la especificación
    private static final float TEMP_VERDE_MAX   = 37.0f;
    private static final float TEMP_NARANJA_MAX = 37.4f;

    private static final int COLOR_VERDE   = Color.parseColor("#4CAF50");
    private static final int COLOR_NARANJA = Color.parseColor("#FF9800");
    private static final int COLOR_ROJO    = Color.parseColor("#F44336");

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Lista plana de items: puede ser Ficha o Registro
    private final List<Object> items = new ArrayList<>();

    public void cargarFichas(List<Ficha> fichas) {
        items.clear();
        // Mostrar fichas en orden inverso (la más reciente arriba)
        for (int i = fichas.size() - 1; i >= 0; i--) {
            Ficha f = fichas.get(i);
            items.add(f);
            for (Registro r : f.getRegistros()) {
                items.add(r);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Ficha ? TIPO_FICHA : TIPO_REGISTRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_FICHA) {
            View v = inflater.inflate(R.layout.item_ficha_header, parent, false);
            return new FichaViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_registro, parent, false);
            return new RegistroViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FichaViewHolder) {
            Ficha ficha = (Ficha) items.get(position);
            FichaViewHolder vh = (FichaViewHolder) holder;
            vh.textTitulo.setText(String.format(Locale.getDefault(),
                    "Ficha %d  ·  %s", ficha.getNumero(),
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date(ficha.getFechaInicio()))));
            if (ficha.isCerrada()) {
                vh.textEstado.setText(R.string.cerrada);
                vh.textEstado.setTextColor(Color.parseColor("#9E9E9E"));
            } else {
                vh.textEstado.setText(R.string.abierta);
                vh.textEstado.setTextColor(COLOR_VERDE);
            }
        } else if (holder instanceof RegistroViewHolder) {
            Registro reg = (Registro) items.get(position);
            RegistroViewHolder vh = (RegistroViewHolder) holder;
            vh.textTemp.setText(String.format(Locale.getDefault(), "%.1f °C", reg.getTemperatura()));
            vh.textFechaHora.setText(SDF.format(new Date(reg.getFechaHora())));

            if (reg.getDescripcion() != null && !reg.getDescripcion().isEmpty()) {
                vh.textDesc.setVisibility(View.VISIBLE);
                vh.textDesc.setText(reg.getDescripcion());
            } else {
                vh.textDesc.setVisibility(View.GONE);
            }

            // Color del indicador según temperatura
            int color;
            if (reg.getTemperatura() <= TEMP_VERDE_MAX)        color = COLOR_VERDE;
            else if (reg.getTemperatura() <= TEMP_NARANJA_MAX) color = COLOR_NARANJA;
            else                                               color = COLOR_ROJO;
            vh.indicador.setBackgroundColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ─── ViewHolders ─────────────────────────────────────────────────────────

    static class FichaViewHolder extends RecyclerView.ViewHolder {
        TextView textTitulo;
        TextView textEstado;

        FichaViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textFichaTitulo);
            textEstado = itemView.findViewById(R.id.textFichaEstado);
        }
    }

    static class RegistroViewHolder extends RecyclerView.ViewHolder {
        View     indicador;
        TextView textTemp;
        TextView textFechaHora;
        TextView textDesc;

        RegistroViewHolder(@NonNull View itemView) {
            super(itemView);
            indicador    = itemView.findViewById(R.id.indicadorTemp);
            textTemp     = itemView.findViewById(R.id.textTemperatura);
            textFechaHora = itemView.findViewById(R.id.textFechaHora);
            textDesc     = itemView.findViewById(R.id.textDescripcionRegistro);
        }
    }
}
