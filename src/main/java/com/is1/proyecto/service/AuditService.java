package com.is1.proyecto.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javalite.activejdbc.LazyList;

import com.is1.proyecto.models.AuditLog;

public class AuditService {
    
    public static void log(int adminId, String accion, int targetId, String detalles) {
        try {
            AuditLog log = new AuditLog();
            log.set("admin_id", adminId);
            log.set("accion", accion);
            log.set("target_id", targetId);
            log.set("detalles", detalles);
            log.saveIt();
            
            System.out.println("[AUDIT] Admin: " + adminId + " | Acción: " + accion + 
                              " | Target: " + targetId + " | Detalles: " + detalles);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }
    
    public static List<Map<String, Object>> getLogs(int limit) {
        try {
            LazyList<AuditLog> logs = AuditLog.findAll()
                .orderBy("timestamp DESC")
                .limit(limit);
            
            return logs.stream().map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("adminId", log.get("admin_id"));
                map.put("accion", log.get("accion"));
                map.put("targetId", log.get("target_id"));
                map.put("detalles", log.get("detalles"));
                map.put("timestamp", log.get("timestamp"));
                return map;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting logs: " + e.getMessage());
            return List.of();
        }
    }
    
    public static List<Map<String, Object>> getAdminLogs(int adminId, int limit) {
        try {
            LazyList<AuditLog> logs = AuditLog.where("admin_id = ?", adminId)
                .orderBy("timestamp DESC")
                .limit(limit);
            
            return logs.stream().map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("adminId", log.get("admin_id"));
                map.put("accion", log.get("accion"));
                map.put("targetId", log.get("target_id"));
                map.put("detalles", log.get("detalles"));
                map.put("timestamp", log.get("timestamp"));
                return map;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting admin logs: " + e.getMessage());
            return List.of();
        }
    }
}