package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = TablesNames.SERVICES)
public class Service {
    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic(optional = false)
    @Column(nullable = false)
    private String name;

    @ManyToMany(mappedBy = "services", cascade = CascadeType.ALL)
    List<Worker> workers;

    public Service(String name) {
        this.name = name;
    }

}
