package br.com.papaya.service;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.dto.response.ProductModerationEventResponse;
import br.com.papaya.enums.ContributorTrustStatus;
import br.com.papaya.enums.ModerationAction;
import br.com.papaya.enums.UserRole;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.exception.ProductNotFoundException;
import br.com.papaya.exception.InvalidModerationStateException;
import br.com.papaya.model.Product;
import br.com.papaya.model.ProductModerationEvent;
import br.com.papaya.model.User;
import br.com.papaya.repository.ProductRepository;
import br.com.papaya.repository.ProductModerationEventRepository;
import br.com.papaya.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductModerationEventRepository moderationEventRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          ProductModerationEventRepository moderationEventRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.moderationEventRepository = moderationEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProductResponse create(@Valid ProductRequest request) {
        return create(request, null);
    }

    @Transactional
    public ProductResponse create(@Valid ProductRequest request, String actorEmail) {
        User actor = findActor(actorEmail);
        boolean autoApproved = actor != null
                && actor.getContributorTrustStatus() == ContributorTrustStatus.VERIFIED;
        Product product = new Product(
                request.name(),
                ProductSource.USER,
                autoApproved ? VerificationStatus.VERIFIED : VerificationStatus.PENDING_REVIEW,
                actor
        );

        Product savedProduct = productRepository.save(product);
        recordEvent(savedProduct, actor, ModerationAction.SUBMITTED, null);
        if (autoApproved) {
            recordEvent(savedProduct, actor, ModerationAction.AUTO_APPROVED, null);
        }

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProductResponse update(Long id, @Valid ProductRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public ProductResponse update(Long id, @Valid ProductRequest request, String actorEmail) {
        User actor = findActor(actorEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (actor != null && actor.getRole() != UserRole.ADMIN
                && (product.getCreatedBy() == null
                || !product.getCreatedBy().getId().equals(actor.getId()))) {
            throw new AccessDeniedException("Você não pode editar este alimento.");
        }

        if (product.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new InvalidModerationStateException(id, product.getVerificationStatus().name());
        }

        product.update(request.name());
        product.resubmit();

        Product updatedProduct = productRepository.save(product);
        recordEvent(updatedProduct, actor, ModerationAction.RESUBMITTED, null);

        return toResponse(updatedProduct);
    }

    @Transactional
    public ProductResponse approve(Long id, String adminEmail) {
        User admin = findActor(adminEmail);
        requireAdministrator(admin);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        requirePendingReview(product);
        product.approve();
        Product approvedProduct = productRepository.save(product);
        recordEvent(approvedProduct, admin, ModerationAction.APPROVED, null);
        return toResponse(approvedProduct);
    }

    @Transactional
    public ProductResponse reject(Long id, String adminEmail, String reason) {
        User admin = findActor(adminEmail);
        requireAdministrator(admin);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        requirePendingReview(product);
        product.reject();
        Product rejectedProduct = productRepository.save(product);
        recordEvent(rejectedProduct, admin, ModerationAction.REJECTED, reason.strip());
        return toResponse(rejectedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductModerationEventResponse> findModerationHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return moderationEventRepository.findAllByProductIdOrderByOccurredAtAscIdAsc(productId)
                .stream()
                .map(event -> new ProductModerationEventResponse(
                        event.getId(), event.getProductIdSnapshot(), event.getAction(),
                        event.getActorUserIdSnapshot(), event.getActorDisplayNameSnapshot(),
                        event.getReason(), event.getProductSnapshot(), event.getOccurredAt()))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
    }

    private User findActor(String actorEmail) {
        if (actorEmail == null) {
            return null;
        }
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
    }

    private void recordEvent(Product product, User actor, ModerationAction action, String reason) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", product.getName());
        snapshot.put("source", product.getSource().name());
        snapshot.put("verificationStatus", product.getVerificationStatus().name());
        moderationEventRepository.save(new ProductModerationEvent(product, actor, action, reason, snapshot));
    }

    private void requirePendingReview(Product product) {
        if (product.getVerificationStatus() != VerificationStatus.PENDING_REVIEW) {
            throw new InvalidModerationStateException(product.getId(), product.getVerificationStatus().name());
        }
    }

    private void requireAdministrator(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Apenas administradores podem moderar alimentos.");
        }
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSource(),
                product.getVerificationStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
