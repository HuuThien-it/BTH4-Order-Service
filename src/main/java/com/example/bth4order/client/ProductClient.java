package com.example.bth4order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductClient {
    private final RestClient client;

    public ProductClient(@Value("${product.service.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ProductSnapshot findProduct(Long id) {
        try {
            ProductSnapshot result = client.get()
                    .uri("/products/{id}", id)
                    .retrieve()
                    .body(ProductSnapshot.class);
            if (result == null || !id.equals(result.id()) || result.name() == null
                    || result.name().isBlank() || result.price() == null
                    || result.price().signum() < 0 || result.quantity() == null
                    || result.quantity() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Invalid response from Product Service for product " + id);
            }
            return result;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Product " + id + " does not exist in Product Service");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Product Service is unavailable or returned an error", ex);
        }
    }
}
