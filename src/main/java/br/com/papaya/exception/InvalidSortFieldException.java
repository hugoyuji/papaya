package br.com.papaya.exception;

import java.util.Set;
import java.util.TreeSet;

public class InvalidSortFieldException extends RuntimeException {

    public InvalidSortFieldException(String field, Set<String> allowedFields) {
        super("Campo de ordenação '%s' inválido. Campos permitidos: %s."
                .formatted(field, String.join(", ", new TreeSet<>(allowedFields))));
    }
}
