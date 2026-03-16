package com.controltemperatura.model;

public class Registro {
    private long fechaHora;    // milisegundos epoch
    private float temperatura;
    private String descripcion;
    private int nroFicha;

    public Registro(int nroFicha, long fechaHora, float temperatura, String descripcion) {
        this.nroFicha = nroFicha;
        this.fechaHora = fechaHora;
        this.temperatura = temperatura;
        this.descripcion = descripcion != null ? descripcion : "";
    }

    public long getFechaHora() { return fechaHora; }
    public void setFechaHora(long fechaHora) { this.fechaHora = fechaHora; }

    public float getTemperatura() { return temperatura; }
    public void setTemperatura(float temperatura) { this.temperatura = temperatura; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getNroFicha() { return nroFicha; }
    public void setNroFicha(int nroFicha) { this.nroFicha = nroFicha; }
}
