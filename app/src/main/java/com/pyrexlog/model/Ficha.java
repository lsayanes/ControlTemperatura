package com.pyrexlog.model;

import java.util.ArrayList;
import java.util.List;

public class Ficha {
    private int numero;
    private long fechaInicio;   // milisegundos epoch
    private boolean cerrada;
    private List<Registro> registros;

    public Ficha(int numero, long fechaInicio, boolean cerrada) {
        this.numero = numero;
        this.fechaInicio = fechaInicio;
        this.cerrada = cerrada;
        this.registros = new ArrayList<>();
    }

    public int getNumero() { return numero; }

    public long getFechaInicio() { return fechaInicio; }

    public boolean isCerrada() { return cerrada; }
    public void setCerrada(boolean cerrada) { this.cerrada = cerrada; }

    public List<Registro> getRegistros() { return registros; }

    public void agregarRegistro(Registro r) {
        registros.add(r);
    }
}
