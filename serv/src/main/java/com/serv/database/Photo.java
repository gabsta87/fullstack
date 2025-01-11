package com.serv.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Photo {
    @Id
    private long id;
    private String fileName;
    private String title;
    private String description;
    private String url;

}
