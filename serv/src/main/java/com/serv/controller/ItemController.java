package com.serv.controller;

import com.serv.common.TablesNames;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class ItemController {
    private final EntityManager entityManager;

    // Fetch all rows from a table
    @GetMapping("/table/{tableName}")
    public List getTableData(@PathVariable String tableName) {
        String query = String.format("SELECT * FROM %s", tableName);
        Query nativeQuery = entityManager.createNativeQuery(query, Map.class);
        System.out.println("Query result size : "+nativeQuery.getResultList().size());
        return nativeQuery.getResultList();
    }

    // Fetch a specific item from a table by id
    @GetMapping("/table/{tableName}/{id}")
    public Map<String, Object> getTableDataById(@PathVariable String tableName, @PathVariable Long id) {
        String query = String.format("SELECT * FROM %s WHERE id = :id", tableName);
        Query nativeQuery = entityManager.createNativeQuery(query, Map.class);
        nativeQuery.setParameter("id", id);
        System.out.println("Query result : "+nativeQuery.getSingleResult());
        return (Map<String, Object>) nativeQuery.getSingleResult();
    }

    @GetMapping("/simpleProfiles")
    public List getSimpleProfiles() {
        System.out.println("simpleProfiles request received");
//        String query = "SELECT id,pseudo,main_photo_id FROM "+ TablesNames.WORKERS +" ORDER BY priority DESC";
        String query = "SELECT * FROM "+ TablesNames.WORKERS;
        Query nativeQuery = entityManager.createNativeQuery(query, Map.class);
        System.out.println(nativeQuery.getResultList().size());
        System.out.println(nativeQuery.getResultList());
        return nativeQuery.getResultList();
    }
}
