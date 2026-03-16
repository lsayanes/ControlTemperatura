package com.controltemperatura.model;

public class Paciente {
    private int id;
    private String nombre;
    private String apellido;
    private int edad;          // -1 si no se especificó
    private String sexo;       // "M", "F", o ""
    private String descripcion;
    private long ultimoAcceso; // milisegundos epoch
    private int alarmaHoras;   // 0 = sin alarma

    public Paciente(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = "";
        this.edad = -1;
        this.sexo = "";
        this.descripcion = "";
        this.ultimoAcceso = System.currentTimeMillis();
        this.alarmaHoras = 0;
    }

    public Paciente(int id, String nombre, String apellido, int edad, String sexo,
                    String descripcion, long ultimoAcceso, int alarmaHoras) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.sexo = sexo;
        this.descripcion = descripcion;
        this.ultimoAcceso = ultimoAcceso;
        this.alarmaHoras = alarmaHoras;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public long getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(long ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public int getAlarmaHoras() { return alarmaHoras; }
    public void setAlarmaHoras(int alarmaHoras) { this.alarmaHoras = alarmaHoras; }

    public String getNombreCompleto() {
        if (apellido == null || apellido.isEmpty()) return nombre;
        return nombre + " " + apellido;
    }
}
