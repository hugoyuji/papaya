package br.com.papaya.service;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.model.Product;
import br.com.papaya.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {

        Product product = new Product(
                request.name(),
                request.barcode(),
                ProductSource.USER,
                VerificationStatus.PENDING_REVIEW
        );

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getBarcode(),
                savedProduct.getSource(),
                savedProduct.getVerificationStatus(),
                savedProduct.getCreatedAt(),
                savedProduct.getUpdatedAt()
        );
    }
}