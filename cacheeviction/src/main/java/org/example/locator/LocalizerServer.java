package org.example.locator;

import lombok.Data;
import org.example.utils.Loggable;
import org.example.utils.ProxyInfo;
import org.example.utils.common.Communicator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

@Data
public class LocalizerServer implements Loggable {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ProxyInfo proxyInfo;

    public LocalizerServer() {
        this.port = 15551;
        this.proxyInfo = new ProxyInfo("26.97.230.179", 15552);
        createServerSocket();
    }

    private void createServerSocket() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("26.97.230.179"));
            logger().info("Servidor Localizador rodando na porta: {}", serverSocket.getLocalPort());
            System.out.println("Servidor Localizador rodando na porta: " + serverSocket.getLocalPort());

            System.out.println("Aguardando conexão de um Cliente...");
            logger().info("Aguardando conexão de um Cliente...");

            while (running) {
                try {
                    Socket client = serverSocket.accept();

                    logger().info("Cliente conectado: {}", client.getInetAddress().getHostAddress());
                    System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());

                    new Thread(() -> handleClient(client)).start();
                } catch (IOException _) {
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao tentar criar o Servidor Localizador!");
            logger().error("Erro ao tentar criar o Servidor Localizador!", e);
            throw new RuntimeException("Erro ao tentar criar o Servidor Localizador!", e);
        } finally {
            stopServer();
        }
    }

    private void handleClient(Socket client) {
        Communicator communicator = new Communicator(client);

        String message;
        System.out.println("Conexão estabelecida com o cliente!");
        logger().info("Conexão estabelecida com o cliente!");
        System.out.println("Enviando mensagem de confirmação para o cliente...");

        communicator.sendTextMessage("OK");

        System.out.println("Aguardando mensagem do cliente...");
        message = communicator.receiveTextMessage();
        System.out.println("Mensagem recebida: " + message);

        logger().info("Localizador recebeu uma Mensagem: {}", message);

        if (message.equals("GET_PROXY")) {
            logger().info("Localizador enviando a mensagem: 26.97.230.179:15556");
            System.out.println("Localizador enviando a mensagem: 26.97.230.179:15556");
            communicator.sendJsonMessage(proxyInfo);
        } else {
            logger().info("Localizador enviando a mensagem: Mensagem inválida!");
            System.out.println("Localizador enviando a mensagem: Mensagem inválida!");
            communicator.sendTextMessage("Mensagem inválida!");
        }
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
                System.out.println("Servidor Localizador encerrado.");
                logger().info("Servidor Localizador encerrado.");
            }
        } catch (IOException e) {
            logger().error("Erro ao fechar o servidor", e);
        }
    }

    public static void main(String[] args) {
        new LocalizerServer();
    }
}