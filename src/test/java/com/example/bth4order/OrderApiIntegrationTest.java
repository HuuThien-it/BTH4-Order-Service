package com.example.bth4order;

import com.example.bth4order.client.ProductClient;
import com.example.bth4order.client.ProductSnapshot;
import com.example.bth4order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepository repository;
    @MockitoBean ProductClient productClient;

    @BeforeEach
    void init() {
        repository.deleteAll();
        when(productClient.findProduct(eq(2L)))
                .thenReturn(new ProductSnapshot(2L, "Laptop Dell", new BigDecimal("100000.00"), 10));
    }

    private String validRequest() {
        return """
                {"customerName":"Nguyen Van A","items":[{"productId":2,"quantity":2}]}
                """;
    }

    @Test
    void createAndGetAndList() throws Exception {
        String response = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(200000))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();
        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).path("id").longValue();
        mvc.perform(get("/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Nguyen Van A"));
        mvc.perform(get("/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.intValue()));
    }

    @Test
    void rejectsOutOfStockAndNegativeQuantities() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName":"A","items":[{"productId":2,"quantity":11}]}
                        """)).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName":"A","items":[{"productId":2,"quantity":-1}]}
                        """)).andExpect(status().isBadRequest());
    }

    @Test
    void mergesDuplicateProductIdsBeforeCheckingStock() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName":"A","items":[{"productId":2,"quantity":6},{"productId":2,"quantity":5}]}
                        """)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateStatusAndDeleteRestrictions() throws Exception {
        String response = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(validRequest())).andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).path("id").longValue();
        mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{"status":"CONFIRMED"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(delete("/orders/{id}", id)).andExpect(status().isConflict());
        mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{"status":"COMPLETED"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{"status":"PENDING"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAndDeletePendingOrder() throws Exception {
        String response = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content(validRequest())).andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).path("id").longValue();
        mvc.perform(put("/orders/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName":"Nguyen Van B","items":[{"productId":2,"quantity":1}]}
                        """)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Nguyen Van B"))
                .andExpect(jsonPath("$.totalAmount").value(100000));
        mvc.perform(delete("/orders/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/orders/{id}", id)).andExpect(status().isNotFound());
    }
}
