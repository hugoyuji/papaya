package br.com.papaya.service;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.exception.ProductNotFoundException;
import br.com.papaya.model.Product;
import br.com.papaya.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("Arroz", "7891234567890", ProductSource.USER, VerificationStatus.PENDING_REVIEW);
    }

    @Test
    void createSavesProductAsUserPendingReviewAndReturnsResponse() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            setId(saved, 1L);
            return saved;
        });

        ProductResponse response = productService.create(new ProductRequest("Arroz", "7891234567890"));

        assertEquals(1L, response.id());
        assertEquals("Arroz", response.name());
        assertEquals("7891234567890", response.barcode());
        assertEquals(ProductSource.USER, response.source());
        assertEquals(VerificationStatus.PENDING_REVIEW, response.verificationStatus());
        verify(productRepository).save(argThat(saved -> saved.getName().equals("Arroz")
                && saved.getBarcode().equals("7891234567890")
                && saved.getSource() == ProductSource.USER
                && saved.getVerificationStatus() == VerificationStatus.PENDING_REVIEW));
    }

    @Test
    void findByIdReturnsProductResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(1L);

        assertEquals("Arroz", response.name());
        assertEquals("7891234567890", response.barcode());
        assertEquals(ProductSource.USER, response.source());
    }

    @Test
    void findByIdThrowsNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productService.findById(99L));

        assertEquals("Produto com ID 99 não foi encontrado.", exception.getMessage());
    }

    @Test
    void findAllReturnsMappedPageOfProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductResponse> responses = productService.findAll(pageable);

        assertEquals(1, responses.getTotalElements());
        assertEquals(1, responses.getContent().size());
        assertEquals("Arroz", responses.getContent().getFirst().name());
        verify(productRepository).findAll(pageable);
    }

    @Test
    void updateChangesNameAndBarcode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.update(1L, new ProductRequest("Feijão", "12345"));

        assertEquals("Feijão", response.name());
        assertEquals("12345", response.barcode());
        assertEquals(ProductSource.USER, response.source());
        verify(productRepository).save(product);
    }

    @Test
    void updateThrowsNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.update(99L, new ProductRequest("Feijão", "12345")));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteRemovesFoundProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteThrowsNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.delete(99L));
        verify(productRepository, never()).delete(any());
    }

    private static void setId(Product product, Long id) {
        try {
            var idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
