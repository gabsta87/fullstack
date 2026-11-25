package com.serv.database.entities;

import com.serv.common.TableNames;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;

@Data
@Entity
@Table(name = TableNames.LOCATIONS)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

    public Location(String name) {
        this.name = name;
    }

    public Location() {}

    private class Department{
        private String name;
        private Collection<Region> regions;
    }

    private class Region{
        private String name;
    }

    public String toString(){
        return name;
    }
}
