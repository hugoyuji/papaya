package br.com.papaya.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 50)
        @Pattern(regexp = "[0-9]*", message = "deve conter apenas dígitos")
        String barcode
) {
    public ProductRequest {
        if (name != null) {
            name = name.strip();
        }
        if (barcode != null) {
            barcode = barcode.strip();
            if (barcode.isEmpty()) {
                barcode = null;
            }
        }
    }
}
