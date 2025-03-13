package org.example.serverproxy;

import java.io.*;
import java.util.*;

public class Authenticator {
    static private final Map<String, String> credentials = new HashMap<>();

    public Authenticator() {
        if (credentials.isEmpty()) 
            loadCredentials("cacheeviction/src/main/java/org/example/serverproxy/credenciais.txt");
    }

    private void loadCredentials(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    credentials.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar credenciais: " + e.getMessage());
        }
    }

    public boolean authenticate(String login, String password) {
        return credentials.containsKey(login) && credentials.get(login).equals(password);
    }

    public void addCredential(String login, String password) {
        credentials.put(login, password);
    }

    public void removeCredential(String login) {
        credentials.remove(login);
    }
}