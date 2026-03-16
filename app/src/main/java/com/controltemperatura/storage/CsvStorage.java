package com.controltemperatura.storage;

import android.content.Context;

import com.controltemperatura.model.Ficha;
import com.controltemperatura.model.Paciente;
import com.controltemperatura.model.Registro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Capa de persistencia en archivos CSV.
 *
 * Archivos generados en getFilesDir():
 *   pacientes.csv          — índice de todos los pacientes
 *   p_{id}.csv             — fichas y registros de un paciente
 *
 * Formato pacientes.csv:
 *   id,nombre,apellido,edad,sexo,descripcion,ultimo_acceso,alarma_horas
 *
 * Formato p_{id}.csv (una fila por ficha o registro):
 *   tipo,campo1,campo2,campo3,campo4
 *   FICHA,{nro},{fecha_inicio_ISO},{abierta|cerrada},
 *   REGISTRO,{fecha_hora_ISO},{temperatura},{descripcion},
 */
public class CsvStorage {

    private static final String ARCHIVO_PACIENTES = "pacientes.csv";
    private static final String PREFIJO_PACIENTE  = "p_";
    private static final String SUFIJO_PACIENTE   = ".csv";

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    private final File directorioBase;

    public CsvStorage(Context context) {
        this.directorioBase = context.getFilesDir();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pacientes
    // ─────────────────────────────────────────────────────────────────────────

    /** Carga todos los pacientes, ordenados por último acceso descendente. */
    public List<Paciente> cargarPacientes() {
        List<Paciente> lista = new ArrayList<>();
        File archivo = new File(directorioBase, ARCHIVO_PACIENTES);
        if (!archivo.exists()) return lista;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) { primeraLinea = false; continue; } // saltar encabezado
                if (linea.trim().isEmpty()) continue;
                String[] c = parsearLineaCsv(linea);
                if (c.length < 8) continue;
                int id           = Integer.parseInt(c[0].trim());
                String nombre    = c[1];
                String apellido  = c[2];
                int edad         = c[3].trim().isEmpty() ? -1 : Integer.parseInt(c[3].trim());
                String sexo      = c[4];
                String desc      = c[5];
                long acceso      = Long.parseLong(c[6].trim());
                int alarmaHoras  = Integer.parseInt(c[7].trim());
                lista.add(new Paciente(id, nombre, apellido, edad, sexo, desc, acceso, alarmaHoras));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        lista.sort((a, b) -> Long.compare(b.getUltimoAcceso(), a.getUltimoAcceso()));
        return lista;
    }

    /**
     * Guarda un paciente. Si ya existe (mismo id) lo actualiza; si id==0 genera uno nuevo.
     * Retorna el id asignado.
     */
    public int guardarPaciente(Paciente paciente) {
        List<Paciente> lista = cargarPacientes();

        if (paciente.getId() == 0) {
            paciente.setId(generarNuevoId(lista));
        }

        boolean encontrado = false;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == paciente.getId()) {
                lista.set(i, paciente);
                encontrado = true;
                break;
            }
        }
        if (!encontrado) lista.add(paciente);

        escribirPacientes(lista);
        return paciente.getId();
    }

    /** Elimina un paciente y su archivo de registros. */
    public void eliminarPaciente(int id) {
        List<Paciente> lista = cargarPacientes();
        lista.removeIf(p -> p.getId() == id);
        escribirPacientes(lista);
        new File(directorioBase, PREFIJO_PACIENTE + id + SUFIJO_PACIENTE).delete();
    }

    private void escribirPacientes(List<Paciente> lista) {
        File archivo = new File(directorioBase, ARCHIVO_PACIENTES);
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println("id,nombre,apellido,edad,sexo,descripcion,ultimo_acceso,alarma_horas");
            for (Paciente p : lista) {
                pw.println(String.join(",",
                        String.valueOf(p.getId()),
                        escaparCsv(p.getNombre()),
                        escaparCsv(p.getApellido()),
                        p.getEdad() == -1 ? "" : String.valueOf(p.getEdad()),
                        escaparCsv(p.getSexo()),
                        escaparCsv(p.getDescripcion()),
                        String.valueOf(p.getUltimoAcceso()),
                        String.valueOf(p.getAlarmaHoras())
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int generarNuevoId(List<Paciente> lista) {
        int maxId = 0;
        for (Paciente p : lista) maxId = Math.max(maxId, p.getId());
        return maxId + 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fichas y Registros
    // ─────────────────────────────────────────────────────────────────────────

    /** Carga todas las fichas (con sus registros) de un paciente. */
    public List<Ficha> cargarFichas(int pacienteId) {
        List<Ficha> fichas = new ArrayList<>();
        File archivo = new File(directorioBase, PREFIJO_PACIENTE + pacienteId + SUFIJO_PACIENTE);
        if (!archivo.exists()) return fichas;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            Ficha fichaActual = null;
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) { primeraLinea = false; continue; }
                if (linea.trim().isEmpty()) continue;
                String[] c = parsearLineaCsv(linea);
                if (c.length < 1) continue;

                if ("FICHA".equals(c[0]) && c.length >= 4) {
                    int nro          = Integer.parseInt(c[1].trim());
                    long fechaInicio = parsearFecha(c[2]);
                    boolean cerrada  = "cerrada".equals(c[3].trim());
                    fichaActual      = new Ficha(nro, fechaInicio, cerrada);
                    fichas.add(fichaActual);

                } else if ("REGISTRO".equals(c[0]) && c.length >= 3 && fichaActual != null) {
                    long fechaHora  = parsearFecha(c[1]);
                    float temp      = Float.parseFloat(c[2].trim());
                    String desc     = c.length >= 4 ? c[3] : "";
                    fichaActual.agregarRegistro(
                            new Registro(fichaActual.getNumero(), fechaHora, temp, desc));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fichas;
    }

    /** Reescribe completamente el archivo de fichas/registros de un paciente. */
    public void guardarFichas(int pacienteId, List<Ficha> fichas) {
        File archivo = new File(directorioBase, PREFIJO_PACIENTE + pacienteId + SUFIJO_PACIENTE);
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            pw.println("tipo,campo1,campo2,campo3,campo4");
            for (Ficha f : fichas) {
                pw.println(String.join(",",
                        "FICHA",
                        String.valueOf(f.getNumero()),
                        formatearFecha(f.getFechaInicio()),
                        f.isCerrada() ? "cerrada" : "abierta",
                        ""
                ));
                for (Registro r : f.getRegistros()) {
                    pw.println(String.join(",",
                            "REGISTRO",
                            formatearFecha(r.getFechaHora()),
                            String.valueOf(r.getTemperatura()),
                            escaparCsv(r.getDescripcion()),
                            ""
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Agrega un registro a la ficha indicada (crea la ficha si no existe). */
    public void agregarRegistro(int pacienteId, Registro registro) {
        List<Ficha> fichas = cargarFichas(pacienteId);
        if (fichas.isEmpty()) {
            fichas.add(new Ficha(1, System.currentTimeMillis(), false));
        }
        for (Ficha f : fichas) {
            if (f.getNumero() == registro.getNroFicha()) {
                f.agregarRegistro(registro);
                break;
            }
        }
        guardarFichas(pacienteId, fichas);
    }

    /**
     * Crea una nueva ficha para el paciente y retorna su número.
     * La ficha queda abierta.
     */
    public int crearNuevaFicha(int pacienteId) {
        List<Ficha> fichas = cargarFichas(pacienteId);
        int nuevoNumero = fichas.isEmpty() ? 1 : fichas.get(fichas.size() - 1).getNumero() + 1;
        fichas.add(new Ficha(nuevoNumero, System.currentTimeMillis(), false));
        guardarFichas(pacienteId, fichas);
        return nuevoNumero;
    }

    /** Marca una ficha como cerrada. */
    public void cerrarFicha(int pacienteId, int nroFicha) {
        List<Ficha> fichas = cargarFichas(pacienteId);
        for (Ficha f : fichas) {
            if (f.getNumero() == nroFicha) {
                f.setCerrada(true);
                break;
            }
        }
        guardarFichas(pacienteId, fichas);
    }

    /** Retorna la ficha abierta más reciente, o null si todas están cerradas. */
    public Ficha getFichaActiva(int pacienteId) {
        List<Ficha> fichas = cargarFichas(pacienteId);
        for (int i = fichas.size() - 1; i >= 0; i--) {
            if (!fichas.get(i).isCerrada()) return fichas.get(i);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades CSV
    // ─────────────────────────────────────────────────────────────────────────

    /** Escapa un valor para CSV: lo rodea de comillas si contiene comas, comillas o saltos. */
    private String escaparCsv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    /** Parsea una línea CSV respetando campos entre comillas. */
    private String[] parsearLineaCsv(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else {
                    enComillas = !enComillas;
                }
            } else if (c == ',' && !enComillas) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(c);
            }
        }
        campos.add(campo.toString());
        return campos.toArray(new String[0]);
    }

    private String formatearFecha(long millis) {
        return SDF.format(new Date(millis));
    }

    private long parsearFecha(String texto) {
        try {
            return SDF.parse(texto.trim()).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }
}
