package com.serv.controllers;

import com.serv.controller.CommonController;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void testGetWorkerServices() throws Exception {
        when(serviceRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/common/services"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetLocationsTree() throws Exception {
        when(zoneRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/common/regions"))
                .andExpect(status().isOk());
    }
}