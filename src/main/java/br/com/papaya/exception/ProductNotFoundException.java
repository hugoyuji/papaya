package br.com.papaya.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Produto com ID %d não foi encontrado.".formatted(id));
    }
}
