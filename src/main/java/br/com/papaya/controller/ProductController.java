package br.com.papaya.controller;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.dto.request.RejectionRequest;
import br.com.papaya.dto.response.ProductModerationEventResponse;
import br.com.papaya.exception.InvalidSortFieldException;
import br.com.papaya.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "source", "verificationStatus", "createdAt", "updatedAt"
    );

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication
    ) {
        return authentication == null
                ? productService.create(request)
                : productService.create(request, authentication.getName());
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping
    public PagedModel<ProductResponse> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty(), ALLOWED_SORT_FIELDS);
            }
        });

        Page<ProductResponse> products = productService.findAll(pageable);
        return new PagedModel<>(products);
    }

    @GetMapping("/{id}/moderation-history")
    public java.util.List<ProductModerationEventResponse> findModerationHistory(@PathVariable Long id) {
        return productService.findModerationHistory(id);
    }

    @PostMapping("/{id}/approve")
    public ProductResponse approve(@PathVariable Long id, Authentication authentication) {
        return productService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public ProductResponse reject(@PathVariable Long id,
                                  @Valid @RequestBody RejectionRequest request,
                                  Authentication authentication) {
        return productService.reject(id, authentication.getName(), request.reason());
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication
    ) {
        return authentication == null
                ? productService.update(id, request)
                : productService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
