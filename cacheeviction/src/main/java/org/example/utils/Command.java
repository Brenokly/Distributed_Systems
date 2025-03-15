package org.example.utils;

import lombok.Getter;

/*
 * Enum que representa os comandos que podem ser enviados pelo cliente para o servidor.
*/

@Getter
public enum Command implements JsonSerializable {
    SEARCH(1, "Buscar OS"),
    REGISTER(2, "Registrar OS"),
    LIST(3, "Listar OS"),
    UPDATE(4, "Atualizar OS"),
    REMOVE(5, "Remover OS"),
    QUANTITY(6, "Quantidade OS"),
    CONECT_LOCATOR(7, "Conectar Localizador"),
    CONECT_PROXY(8, "Conectar Proxy"),
    EXIT(0, "Sair"),
    DISCONECT(9, "Desconectar"),
    AUTHENTICATE(10, "Autenticar"),
    ERROR(500, "Erro"),
    INVALID(-1, "Inválido"),
    SUCCESS(200, "Sucesso");

    private final int code;
    private final String description;

    Command(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Command fromCode(int code) {
        for (Command command : Command.values()) {
            if (command.code == code) {
                return command;
            }
        }
        return null;
    }
}