package org.example.utils;

import lombok.Getter;

@Getter
public enum Command implements JsonSerializable {
    SEARCH(1),
    REGISTER(2),
    LIST(3),
    UPDATE(4),
    REMOVE(5),
    QUANTITY(6),
    CONECT_LOCATOR(7),
    CONECT_PROXY(8),
    EXIT(0),
    DISCONECT(9);

    private final int code;

    Command(int code) {
        this.code = code;
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