package br.com.papaya.model;

import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 50)
    @Pattern(regexp = "[0-9]*")
    @Column(length = 50)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 50)
    private ProductSource source;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product(
            String name,
            String barcode,
            ProductSource source,
            VerificationStatus verificationStatus
    ) {
        this.name = normalizeName(name);
        this.barcode = normalizeBarcode(barcode);
        this.source = source;
        this.verificationStatus = verificationStatus;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String name, String barcode) {
        this.name = normalizeName(name);
        this.barcode = normalizeBarcode(barcode);
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.strip();
    }

    private static String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return null;
        }
        String normalized = barcode.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
