package com.serv.dto;

import com.serv.database.entities.GeographicZone;
import lombok.Data;
import java.util.List;

@Data
public class GeographicZoneDTO {
    private Integer id;
    private String name;
    private List<GeographicZoneDTO> subZones;

    public static GeographicZoneDTO from(GeographicZone zone) {
        GeographicZoneDTO dto = new GeographicZoneDTO();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        if (zone.getSubZones() != null) {
            dto.setSubZones(zone.getSubZones().stream().map(GeographicZoneDTO::from).toList());
        }

        return dto;
    }
}
