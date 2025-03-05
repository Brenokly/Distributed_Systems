package org.example.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class Menu implements JsonSerializable {
    private String menu;
    private final Map<Command, Runnable> actions = new TreeMap<>(Comparator.comparingInt(Command::getCode));

    public Menu() {
        menu = "";
    }

    public List<Command> getCommands() {
        return new ArrayList<>(actions.keySet());
    }

    public void put(Command option, Runnable action) {
        actions.put(option, action);
        buildMenuString();
    }

    public void remove(Command option) {
        actions.remove(option);
        buildMenuString();
    }

    public Runnable get(int option) {
        return actions.get(Command.fromCode(option));
    }

    public Runnable get(Command option) {
        return actions.get(option);
    }

    private void buildMenuString() {
        StringBuilder menuBuilder = new StringBuilder();

        actions.forEach((cmd, _) ->
                menuBuilder.append(cmd.getCode()).append(" - ").append(cmd.getDescription()).append("\n")
        );

        menu = menuBuilder.toString();
    }

    public void clearMenu() {
        menu = "";
        actions.clear();
    }

    public void showMenu() {
        System.out.println(menu);
    }

    public boolean menuIsValid() {
        return menu != null && !menu.isEmpty();
    }
}