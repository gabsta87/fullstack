package com.serv.database;

import com.serv.common.TableNames;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TableNamesTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Every table created by Hibernate is declared in TableNames, and the names never change")
    void tablesMatchTableNames() throws IllegalAccessException {
        Set<String> declared = new java.util.TreeSet<>();
        for (Field field : TableNames.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                declared.add((String) field.get(null));
            }
        }
        Set<String> created = jdbc.queryForList(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_SCHEMA) = 'public'", String.class)
                .stream().map(String::toLowerCase)
                .filter(name -> !name.startsWith("ht_") && !name.startsWith("hte_")) // Hibernate temporary tables
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(created).isEqualTo(declared);
        // Existing names (production data lives in them): renaming one would silently start an empty table.
        assertThat(declared).containsAll(Arrays.asList("users", "workers", "clients", "admin", "super_admin", "photos",
                "videos", "services", "geographic_zones", "comments", "reports", "certification_requests"));
    }
}
