package br.com.papaya.exception;

public class InvalidModerationStateException extends RuntimeException {
    public InvalidModerationStateException(Long id, String state) {
        super("O alimento %d não pode receber esta decisão no estado %s.".formatted(id, state));
    }
}
