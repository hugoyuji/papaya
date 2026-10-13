package br.com.papaya.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank
        @Size(max = 200)
        String name
) {
    public ProductRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
