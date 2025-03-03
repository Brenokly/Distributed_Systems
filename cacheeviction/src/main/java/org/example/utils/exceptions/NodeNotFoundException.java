package org.example.utils.exceptions;

public class NodeNotFoundException extends Exception { // Quando o nó não é encontrado
    public NodeNotFoundException(String message) {
        super(message);
    }
}
