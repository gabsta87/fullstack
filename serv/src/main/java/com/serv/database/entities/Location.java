package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = TablesNames.LOCATIONS)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String region;
}
