package org.example.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Menu {
    private String menu;
    private final Map<Command, Runnable> actions = new HashMap<>();

    public void updateActions(Map<Command, Runnable> menuOptions) {
        actions.putAll(menuOptions);
    }

    public void put(Command option, Runnable action) {
        actions.put(option, action);
    }

    public Runnable get(int option) {
        return actions.get(Command.fromCode(option));
    }

    private void buildMenuString(Map<Command, String> menuOptions) {
        StringBuilder menuBuilder = new StringBuilder();
        menuOptions.forEach((option, description) -> menuBuilder.append(option).append(" - ").append(description).append("\n"));
        menu = menuBuilder.toString();
    }

    public void clearMenu() {
        menu = null;
        actions.clear();
    }

    public void updateMenu(Map<Command, String> menuOptions) {
        clearMenu();
        buildMenuString(menuOptions);
    }

    public void showMenu() {
        System.out.println(menu);
    }

    public boolean menuIsValid() {
        return menu != null && !menu.isEmpty();
    }
}