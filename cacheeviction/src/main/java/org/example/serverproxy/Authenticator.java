package org.example.serverproxy;

import java.io.*;
import java.util.*;

public class Authenticator {
    private static final Map<String, String> credentials = new HashMap<>();         // login -> password
    private static final Map<String, Long> authenticatedClients = new HashMap<>();  // ip -> expiration time
    private static final long EXPIRATION_TIME_MS = 30 * 60 * 1000;                  // 30 minutos

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

    public boolean authenticate(String login, String password, String ip) {
        removeExpiredClients();

        if (credentials.containsKey(login) && credentials.get(login).equals(password)) {
            authenticatedClients.put(ip, System.currentTimeMillis() + EXPIRATION_TIME_MS);
            return true;
        }
        return false;
    }

    public boolean isAuthenticated(String ip) {
        removeExpiredClients();
        return authenticatedClients.containsKey(ip);
    }

    private void removeExpiredClients() {
        long now = System.currentTimeMillis();
        authenticatedClients.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    public void removeAuthenticatedClient(String ip) {
        authenticatedClients.remove(ip);
    }

    public void addCredential(String login, String password) {
        credentials.put(login, password);
    }

    public void removeCredential(String login) {
        credentials.remove(login);
    }
}