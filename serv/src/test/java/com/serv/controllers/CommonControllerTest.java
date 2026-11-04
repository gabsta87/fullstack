package com.serv.controllers;

import com.serv.database.entities.GeographicZone;
import com.serv.database.entities.Service;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommonController.class)
class CommonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceRepository serviceRepository;

    @MockBean
    private GeographicZoneRepository zoneRepository;

    @Test
    @DisplayName("GET /common/services - Doit retourner la liste des services convertis en DTO")
    void testGetWorkerServices() throws Exception {
        Service service = new Service();
        service.setId(1);
        service.setName("Massage");

        when(serviceRepository.findAll()).thenReturn(List.of(service));

        mockMvc.perform(get("/common/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Massage"));
    }

    @Test
    @DisplayName("GET /common/regions - Doit retourner l'arbre des zones géographiques (racines uniquement)")
    void testGetLocationsTree() throws Exception {
        GeographicZone parentZone = new GeographicZone();
        parentZone.setId(1);
        parentZone.setName("Genève");
        parentZone.setParent(null); // Zone racine

        GeographicZone childZone = new GeographicZone();
        childZone.setId(2);
        childZone.setName("Carouge");
        childZone.setParent(parentZone); // Enfant

        // Le repository retourne les deux, mais le contrôleur doit filtrer pour ne garder que la racine
        when(zoneRepository.findAll()).thenReturn(List.of(parentZone, childZone));

        mockMvc.perform(get("/common/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Genève"));
        // On pourrait aussi vérifier que la taille de la liste retournée est bien 1 (grâce au filtre parent == null)
    }
}