package org.example.utils.tree;

public enum RotationType {
    LEFT, RIGHT, LEFT_RIGHT, RIGHT_LEFT, NONE;

    // método para retornar em string o tipo de rotação
    public String toString() {
        return switch (this) {
            case LEFT -> "Rotacao simples a esquerda";
            case RIGHT -> "Rotacao simples a direita";
            case RIGHT_LEFT -> "Rotacao dupla a esquerda";
            case LEFT_RIGHT -> "Rotacao dupla a direita";
            case NONE -> "Nenhuma rotacao";
        };
    }
}