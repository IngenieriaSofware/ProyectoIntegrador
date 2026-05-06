package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.PeriodoLectivo;

public class PeriodoService {

    public List<Map<String, Object>> listarPeriodos() {
        List<Map> rows = Base.findAll(
            "SELECT * FROM periodos_lectivos ORDER BY anio DESC, cuatrimestre DESC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(toMap(row));
        }
        return result;
    }

    public Map<String, Object> getPeriodoActivo() {
        List<Map> rows = Base.findAll("SELECT * FROM periodos_lectivos WHERE activo = 1 LIMIT 1");
        if (rows.isEmpty()) return null;
        return toMap(rows.get(0));
    }

    public Map<String, Object> getPeriodoById(int id) {
        PeriodoLectivo p = PeriodoLectivo.findById(id);
        if (p == null) return null;
        return buildMap(p);
    }

    public Map<String, Object> crearPeriodo(int anio, int cuatrimestre, String descripcion) {
        Map<String, Object> result = new HashMap<>();

        if (anio < 2000 || anio > 2100) {
            result.put("success", false);
            result.put("field", "anio");
            result.put("message", "El año debe estar entre 2000 y 2100.");
            return result;
        }
        if (cuatrimestre != 1 && cuatrimestre != 2) {
            result.put("success", false);
            result.put("field", "cuatrimestre");
            result.put("message", "El cuatrimestre debe ser 1 o 2.");
            return result;
        }

        if (PeriodoLectivo.count("anio = ? AND cuatrimestre = ?", anio, cuatrimestre) > 0) {
            result.put("success", false);
            result.put("message", "Ya existe un período para " + anio + " - " + (cuatrimestre == 1 ? "1er" : "2do") + " cuatrimestre.");
            return result;
        }

        String label = anio + " — " + (cuatrimestre == 1 ? "Primer" : "Segundo") + " Cuatrimestre";
        PeriodoLectivo p = new PeriodoLectivo();
        p.setAnio(anio);
        p.setCuatrimestre(cuatrimestre);
        p.setDescripcion(descripcion != null && !descripcion.trim().isEmpty() ? descripcion.trim() : label);
        p.setActivo(false);
        p.saveIt();

        result.put("success", true);
        result.put("id", p.getLongId());
        result.put("message", "Período " + label + " creado.");
        return result;
    }

    public Map<String, Object> activarPeriodo(int id) {
        Map<String, Object> result = new HashMap<>();
        PeriodoLectivo p = PeriodoLectivo.findById(id);
        if (p == null) {
            result.put("success", false);
            result.put("message", "Período no encontrado.");
            return result;
        }
        if (p.isActivo()) {
            result.put("success", false);
            result.put("message", "El período ya está activo.");
            return result;
        }
        // Desactivar todos los demás (solo uno activo a la vez)
        Base.exec("UPDATE periodos_lectivos SET activo = 0 WHERE activo = 1");
        p.setActivo(true);
        p.saveIt();

        result.put("success", true);
        result.put("message", "Período activado: " + p.getLabel());
        return result;
    }

    public Map<String, Object> desactivarPeriodo(int id) {
        Map<String, Object> result = new HashMap<>();
        PeriodoLectivo p = PeriodoLectivo.findById(id);
        if (p == null) {
            result.put("success", false);
            result.put("message", "Período no encontrado.");
            return result;
        }
        if (!p.isActivo()) {
            result.put("success", false);
            result.put("message", "El período ya está inactivo.");
            return result;
        }
        p.setActivo(false);
        p.saveIt();

        result.put("success", true);
        result.put("message", "Período desactivado.");
        return result;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private Map<String, Object> toMap(Map row) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.get("id"));
        m.put("anio", row.get("anio"));
        m.put("cuatrimestre", row.get("cuatrimestre"));
        m.put("descripcion", row.get("descripcion"));
        boolean activo = toBoolean(row.get("activo"));
        m.put("activo", activo);
        int c = ((Number) row.get("cuatrimestre")).intValue();
        m.put("label", row.get("anio") + " — " + (c == 1 ? "Primer" : "Segundo") + " Cuatrimestre");
        return m;
    }

    private Map<String, Object> buildMap(PeriodoLectivo p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getLongId());
        m.put("anio", p.getAnio());
        m.put("cuatrimestre", p.getCuatrimestre());
        m.put("descripcion", p.getDescripcion());
        m.put("activo", p.isActivo());
        m.put("label", p.getLabel());
        return m;
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }
}
