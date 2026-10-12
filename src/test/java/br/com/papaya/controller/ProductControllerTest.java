package br.com.papaya.controller;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.exception.ProductNotFoundException;
import br.com.papaya.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String BASE_URL = "/api/v1/products";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createReturnsCreatedAndProductBody() throws Exception {
        when(productService.create(new ProductRequest("Arroz", "7891234567890"))).thenReturn(response(1L, "Arroz"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\",\"barcode\":\"7891234567890\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.barcode").value("7891234567890"))
                .andExpect(jsonPath("$.source").value("USER"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_REVIEW"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"barcode\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void createTreatsMissingBarcodeAsNull() throws Exception {
        when(productService.create(new ProductRequest("Arroz", null))).thenReturn(response(1L, "Arroz", null));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(nullValue()));

        verify(productService).create(new ProductRequest("Arroz", null));
    }

    @Test
    void createTrimsBarcodeAndTreatsWhitespaceOnlyBarcodeAsNull() throws Exception {
        when(productService.create(new ProductRequest("Arroz", null))).thenReturn(response(1L, "Arroz", null));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\",\"barcode\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(nullValue()));

        verify(productService).create(new ProductRequest("Arroz", null));
    }

    @Test
    void createTrimsSpacesAroundBarcode() throws Exception {
        when(productService.create(new ProductRequest("Arroz", "7891234567890")))
                .thenReturn(response(1L, "Arroz", "7891234567890"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\",\"barcode\":\" 7891234567890 \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value("7891234567890"));

        verify(productService).create(new ProductRequest("Arroz", "7891234567890"));
    }

    @Test
    void createRejectsBarcodeLongerThan50Characters() throws Exception {
        String barcode = "1".repeat(51);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\",\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.barcode").exists());

        verifyNoInteractions(productService);
    }

    @Test
    void findByIdReturnsProductBody() throws Exception {
        when(productService.findById(1L)).thenReturn(response(1L, "Arroz"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arroz"));
    }

    @Test
    void findByIdReturnsNotFoundForMissingProduct() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Produto com ID 99 não foi encontrado."));
    }

    @Test
    void findByIdRejectsNonNumericIdWithProblemDetail() throws Exception {
        mockMvc.perform(get(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("O parâmetro 'id' possui formato inválido."));

        verifyNoInteractions(productService);
    }

    @Test
    void findAllReturnsProductList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(response(1L, "Arroz"), response(2L, "Feijão")));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Arroz"))
                .andExpect(jsonPath("$[1].name").value("Feijão"));
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        when(productService.update(1L, new ProductRequest("Feijão", "12345"))).thenReturn(response(1L, "Feijão"));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Feijão\",\"barcode\":\"12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Feijão"));
    }

    @Test
    void updateRejectsMissingName() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"12345\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    private static ProductResponse response(Long id, String name) {
        return response(id, name, "7891234567890");
    }

    private static ProductResponse response(Long id, String name, String barcode) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 12, 0);
        return new ProductResponse(id, name, barcode, ProductSource.USER,
                VerificationStatus.PENDING_REVIEW, timestamp, timestamp);
    }
}
