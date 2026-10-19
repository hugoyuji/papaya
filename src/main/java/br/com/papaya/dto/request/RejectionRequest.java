package br.com.papaya.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectionRequest(@NotBlank @Size(max = 1000) String reason) {
    public RejectionRequest {
        if (reason != null) {
            reason = reason.strip();
        }
    }
}
