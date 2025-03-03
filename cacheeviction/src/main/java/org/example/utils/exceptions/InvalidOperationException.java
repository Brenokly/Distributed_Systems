package org.example.utils.exceptions;

public class InvalidOperationException extends RuntimeException { // Quando a operação não é válida
    public InvalidOperationException(String message) { // Exemplo: tentar remover um nó que não existe
        super(message);
    }
}