package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Correlatividad;

public class CorrelatividadService {

    // ── GRAFO DE CORRELATIVIDADES ────────────────────────────────

    public Map<String, Object> agregarCorrelatividad(int origenMpId, int destinoMpId, String condicion) {
        Map<String, Object> result = new HashMap<>();

        if (origenMpId <= 0 || destinoMpId <= 0) {
            result.put("success", false);
            result.put("message", "Los identificadores de asignación al plan son obligatorios.");
            return result;
        }

        if (condicion == null || condicion.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "La condición es obligatoria.");
            return result;
        }
        condicion = condicion.toUpperCase().trim();

        if (!"REGULAR".equals(condicion) && !"APROBADA".equals(condicion)) {
            result.put("success", false);
            result.put("message", "La condición debe ser REGULAR o APROBADA.");
            return result;
        }

        if (origenMpId == destinoMpId) {
            result.put("success", false);
            result.put("message", "Una materia no puede ser correlativa de sí misma.");
            return result;
        }

        String sqlCheck = "SELECT id, plan_estudio_id, anio_plan, cuatrimestre, activa FROM materias_planes WHERE id = ?";
        List<Map> mpOrigenList = Base.findAll(sqlCheck, origenMpId);
        List<Map> mpDestinoList = Base.findAll(sqlCheck, destinoMpId);

        if (mpOrigenList.isEmpty() || mpDestinoList.isEmpty()) {
            result.put("success", false);
            result.put("message", "Las asignaciones de las materias en el plan de estudios no existen.");
            return result;
        }

        Map rowOrigen = mpOrigenList.get(0);
        Map rowDestino = mpDestinoList.get(0);

        if (!toBoolean(rowOrigen.get("activa")) || !toBoolean(rowDestino.get("activa"))) {
            result.put("success", false);
            result.put("message", "No se puede establecer la relación porque una de las asignaciones está inactiva.");
            return result;
        }

        int planOrig = ((Number) rowOrigen.get("plan_estudio_id")).intValue();
        int planDest = ((Number) rowDestino.get("plan_estudio_id")).intValue();

        if (planOrig != planDest) {
            result.put("success", false);
            result.put("message", "No podés cruzar correlatividades entre carreras o planes distintos.");
            return result;
        }

        int anioOrig = ((Number) rowOrigen.get("anio_plan")).intValue();
        int anioDest = ((Number) rowDestino.get("anio_plan")).intValue();

        if (anioOrig > anioDest) {
            result.put("success", false);
            result.put("message", "La materia requisito pertenece a un año posterior.");
            return result;
        } else if (anioOrig == anioDest) {
            String cuatO = (String) rowOrigen.get("cuatrimestre");
            String cuatD = (String) rowDestino.get("cuatrimestre");
            
            if (!("PRIMERO".equals(cuatO) && "SEGUNDO".equals(cuatD))) {
                result.put("success", false);
                result.put("message", "En un mismo año, solo una materia del primer cuatrimestre puede ser requisito de una del segundo.");
                return result;
            }
        }

        long duplicados = Correlatividad.count(
            "materia_plan_origen_id = ? AND materia_plan_destino_id = ?", 
            origenMpId, destinoMpId
        );
        if (duplicados > 0) {
            result.put("success", false);
            result.put("message", "Esta regla de correlatividad ya se encuentra registrada para este plan.");
            return result;
        }

        if (detectaCiclo(origenMpId, destinoMpId)) {
            result.put("success", false);
            result.put("message", "Agregar este requisito generaría un bucle infinito en este plan de estudios.");
            return result;
        }

        Correlatividad nueva = new Correlatividad();
        nueva.setMateriaPlanOrigenId(origenMpId);
        nueva.setMateriaPlanDestinoId(destinoMpId);
        nueva.setCondicion(condicion);
        nueva.saveIt();

        result.put("success", true);
        result.put("message", "Correlatividad registrada correctamente para el plan.");
        return result;
    }

    public Map<String, Object> eliminarCorrelatividad(int id) {
        Map<String, Object> result = new HashMap<>();
        Correlatividad c = Correlatividad.findById(id);
        
        if (c == null) {
            result.put("success", false);
            result.put("message", "La regla de correlatividad no existe.");
            return result;
        }

        c.delete();
        result.put("success", true);
        result.put("message", "Correlatividad de-registrada correctamente.");
        return result;
    }

    public Map<String, Object> getCorrelatividadesDelPlan(int materiaPlanId) {
        Map<String, Object> result = new HashMap<>();

        String sqlAnteriores = 
            "SELECT c.id, c.materia_plan_origen_id AS mp_id, m.nombre, m.codigo, c.condicion " +
            "FROM correlatividades c " +
            "JOIN materias_planes mp ON c.materia_plan_origen_id = mp.id " +
            "JOIN materias m ON mp.materia_id = m.id " +
            "WHERE c.materia_plan_destino_id = ? " +
            "ORDER BY m.nombre ASC";
        
        List<Map> rowsAnteriores = Base.findAll(sqlAnteriores, materiaPlanId);
        List<Map<String, Object>> anteriores = mapearFilas(rowsAnteriores);

        String sqlPosteriores = 
            "SELECT c.id, c.materia_plan_destino_id AS mp_id, m.nombre, m.codigo, c.condicion " +
            "FROM correlatividades c " +
            "JOIN materias_planes mp ON c.materia_plan_destino_id = mp.id " +
            "JOIN materias m ON mp.materia_id = m.id " +
            "WHERE c.materia_plan_origen_id = ? " +
            "ORDER BY m.nombre ASC";
            
        List<Map> rowsPosteriores = Base.findAll(sqlPosteriores, materiaPlanId);
        List<Map<String, Object>> posteriores = mapearFilas(rowsPosteriores);

        result.put("materiaPlanId", materiaPlanId);
        result.put("anteriores", anteriores);
        result.put("posteriores", posteriores);
        return result;
    }

    public boolean detectaCiclo(int origenMpId, int destinoMpId) {
        if (origenMpId == destinoMpId) return true;

        Queue<Integer> cola = new LinkedList<>();
        Set<Integer> visitados = new HashSet<>();

        cola.add(destinoMpId);
        visitados.add(destinoMpId);

        while (!cola.isEmpty()) {
            int actualMpId = cola.poll();

            if (actualMpId == origenMpId) {
                return true;
            }

            List<Correlatividad> sucesores = Correlatividad.where("materia_plan_origen_id = ?", actualMpId);
            for (Correlatividad c : sucesores) {
                int siguienteDestinoMpId = c.getMateriaPlanDestinoId();
                if (!visitados.contains(siguienteDestinoMpId)) {
                    visitados.add(siguienteDestinoMpId);
                    cola.add(siguienteDestinoMpId);
                }
            }
        }
        return false;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private List<Map<String, Object>> mapearFilas(List<Map> rows) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", row.get("id"));
            dto.put("materiaPlanId", row.get("mp_id"));
            dto.put("codigo", row.get("codigo"));
            dto.put("nombre", row.get("nombre"));
            String cond = (String) row.get("condicion");
            dto.put("condicion", cond);
            dto.put("condicionLabel", condicionLabel(cond));
            lista.add(dto);
        }
        return lista;
    }

    private String condicionLabel(String c) {
        if ("REGULAR".equals(c)) return "Regular";
        if ("APROBADA".equals(c)) return "Aprobada";
        return c;
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }
}