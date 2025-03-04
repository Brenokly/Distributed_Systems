package org.example.serverproxy;

import org.example.locator.LocalizerServer;
import org.example.utils.Loggable;
import org.example.utils.Menu;
import org.example.utils.ProxyInfo;
import org.example.utils.common.Communicator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Proxy implements Loggable {
    private final int port;
    private final ProxyInfo serverInfo;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final Menu actions;

    public Proxy() {
        this.port = 15552;
        //this.proxyInfo = new ProxyInfo("26.97.230.179", 15553);
        this.serverInfo = new ProxyInfo("localhost", 15553);
        this.actions = new Menu();
        createServerSocket();
    }

    private void createServerSocket() {
        try {
            // serverSocket = new ServerSocket(port, 50, InetAddress.getByName("26.97.230.179")); // RemoteHost
            serverSocket = new ServerSocket(port); // LocalHost
            info("Servidor Localizador rodando na porta: " + serverSocket.getLocalPort());

            while (running) {
                try {
                    info("Servidor Aguardando conexão de um Cliente...");
                    startCommandListener();
                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    erro("Erro ao tentar conectar com o Cliente: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor Localizador!");
            throw new RuntimeException("Erro ao tentar criar o Servidor Localizador!", e);
        } finally {
            stopServer();
        }
    }

    private void handleClient(Socket client) {
        Communicator communicator = new Communicator(client, "Localizador");

    }

    private void startCommandListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Digite 'stop' para encerrar o Servidor Localizador: ");
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("stop")) {
                    stopServer();
                    break;
                }
            }
        }).start();
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                info("Servidor Localizador encerrado.");
            }
        } catch (IOException e) {
            erro("Erro ao fechar o servidor" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new LocalizerServer();
    }
}