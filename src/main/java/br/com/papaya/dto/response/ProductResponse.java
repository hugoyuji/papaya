package br.com.papaya.dto.response;

import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String barcode,
        ProductSource source,
        VerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
