package org.example.locator;

import lombok.Data;
import org.example.utils.Loggable;
import org.example.utils.common.Communicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.Socket;

@Data
public class LocalizerServer implements Loggable {
    private static final Logger log = LoggerFactory.getLogger(LocalizerServer.class);
    private final int port;
    private ServerSocket serverSocket;

    public LocalizerServer() {
        this.port = 12345;
        createServerSocket();
    }

    private void createServerSocket() {
        try {
            serverSocket = new ServerSocket(port);
            logger().info("Servidor Localizador criado com sucesso!");
            System.out.println("Servidor Localizador criado com sucesso!");

            while (true) {
                System.out.println("Aguardando conexão de um Cliente...");
                logger().info("Aguardando conexão de um Cliente...");

                Socket client = serverSocket.accept();

                if (client.isConnected()) {
                    logger().info("Cliente conectado: {}", client.getInetAddress().getHostAddress());
                    System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());

                    // Criar uma thread para cada cliente
                    new Thread(() -> handleClient(client)).start();
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao tentar criar o Servidor Localizador!");
            logger().error("Erro ao tentar criar o Servidor Localizador!", e);
            throw new RuntimeException("Erro ao tentar criar o Servidor Localizador!", e);
        }
    }

    private void handleClient(Socket client) {
        Communicator communicator = new Communicator();
        communicator.connect(client.getInetAddress().getHostAddress(), client.getPort());

        String message = "";
        while (!message.equals("GET_PROXY")) {
            message = communicator.receiveTextMessage();

            System.out.println("Localizador recebeu uma Mensagem: " + message);
            logger().info("Localizador recebeu uma Mensagem: {}", message);

            if (message.equals("GET_PROXY")) {
                logger().info("Localizador enviando a mensagem: localhost:12346");
                System.out.println("Localizador enviando a mensagem: localhost:12346");
                communicator.sendTextMessage("localhost:12346");
            }
        }

        communicator.disconnect();
    }
}