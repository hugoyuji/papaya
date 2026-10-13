package br.com.papaya.controller;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.exception.ProductNotFoundException;
import br.com.papaya.service.ProductService;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

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
        when(productService.create(new ProductRequest("Arroz"))).thenReturn(response(1L, "Arroz"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.source").value("USER"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_REVIEW"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void createReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("O corpo da requisição está ausente ou malformado."));

        verifyNoInteractions(productService);
    }

    @Test
    void createReturnsConflictForDataIntegrityViolationWithoutLeakingDetails() throws Exception {
        when(productService.create(new ProductRequest("Arroz")))
                .thenThrow(new DataIntegrityViolationException("products_name_not_blank"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arroz\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A operação não pode ser concluída porque os dados entram em conflito com o estado atual."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("products_name_not_blank"))));
    }

    @Test
    void createTrimsNameBeforePassingItToService() throws Exception {
        when(productService.create(new ProductRequest("Arroz"))).thenReturn(response(1L, "Arroz"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Arroz  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arroz"));

        verify(productService).create(new ProductRequest("Arroz"));
    }

    @Test
    void createAcceptsNameAtMaximumLength() throws Exception {
        String name = "A".repeat(200);
        when(productService.create(new ProductRequest(name))).thenReturn(response(1L, name));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createRejectsNameLongerThan200Characters() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "A".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(productService);
    }







    @Test
    void findByIdReturnsProductBody() throws Exception {
        when(productService.findById(1L)).thenReturn(response(1L, "Arroz"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T15:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T15:00:00Z"));
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
    void unexpectedExceptionReturnsGenericInternalServerError() throws Exception {
        when(productService.findById(1L)).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail")
                        .value("Ocorreu um erro interno. Tente novamente mais tarde."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalhe interno"))));
    }

    @Test
    void findAllReturnsRequestedPageOfProducts() throws Exception {
        when(productService.findAll(any())).thenReturn(new PageImpl<>(
                List.of(response(1L, "Arroz"), response(2L, "Feijão")),
                PageRequest.of(0, 2, Sort.by("id").ascending()),
                3
        ));

        mockMvc.perform(get(BASE_URL).queryParam("page", "0").queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arroz"))
                .andExpect(jsonPath("$.content[1].name").value("Feijão"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(productService).findAll(argThat(pageable -> pageable.getPageNumber() == 0
                && pageable.getPageSize() == 2
                && pageable.getSort().getOrderFor("id") != null));
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        when(productService.update(1L, new ProductRequest("Feijão"))).thenReturn(response(1L, "Feijão"));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Feijão\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Feijão"));
    }

    @Test
    void updateRejectsMissingName() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
        Instant timestamp = Instant.parse("2026-01-01T15:00:00Z");
        return new ProductResponse(id, name, ProductSource.USER,
                VerificationStatus.PENDING_REVIEW, timestamp, timestamp);
    }
}
