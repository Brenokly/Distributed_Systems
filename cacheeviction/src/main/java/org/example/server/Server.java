package org.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.utils.Command;
import org.example.utils.JsonSerializable;
import org.example.utils.Loggable;
import org.example.utils.Menu;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;
import org.example.utils.exceptions.InvalidOperationException;
import org.example.utils.exceptions.NodeAlreadyExistsException;
import org.example.utils.exceptions.NodeNotFoundException;
import org.example.utils.tree.TreeAVL;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.utils.Command.*;

public class Server implements Loggable, JsonSerializable {
    private final int port;                                                                      // Porta do servidor
    private final Menu actions;                                                                  // Menu de ações do servidor
    private volatile TreeAVL treeAVL;                                                            // Árvore AVL de dados
    private ServerSocket serverSocket;                                                           // Socket do servidor
    private final Object treeLock = new Object();                                                // Lock para sincronização da árvore
    private volatile AtomicBoolean running = new AtomicBoolean(true);                   // Flag de controle de execução
    private static final ThreadLocal<Communicator> clientCommunicator = new ThreadLocal<>();     // Comunicador do cliente

    public Server() {
        this.port = 15553;
        this.treeAVL = new TreeAVL();
        this.actions = new Menu();
        initializerDefaultActions();
        createServerSocket();
    }

    private void initializerDefaultActions() {
        actions.put(SEARCH, () -> searchOS(clientCommunicator.get()));
        actions.put(REGISTER, () -> registerOS(clientCommunicator.get()));
        actions.put(LIST, () -> listOS(clientCommunicator.get()));
        actions.put(UPDATE, () -> updateOS(clientCommunicator.get()));
        actions.put(REMOVE, () -> removeOS(clientCommunicator.get()));
        actions.put(QUANTITY, () -> quantityRecords(clientCommunicator.get()));
        actions.put(DISCONECT, () -> disconnectProgram(clientCommunicator.get()));
    }

    private void createServerSocket() {
        try {
            // serverSocket = new ServerSocket(port, 50, InetAddress.getByName("26.97.230.179")); // RemoteHost
            serverSocket = new ServerSocket(port); // LocalHost
            info("Servidor Principal rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar o Servidor ServerMain");


            while (running.get()) {
                try {
                    info("Servidor Principal Aguardando conexão de um Cliente...");
                    startCommandListener();
                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    erro("Erro ao tentar conectar com o Cliente: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor ServerMain!");
            throw new RuntimeException("Erro ao tentar criar o Servidor ServerMain!", e);
        } finally {
            stopServer();
        }
    }

    private void handleClient(Socket client) {
        if (client == null) {
            erro("Cliente nulo, não é possível continuar!");
            return;
        }

        Communicator communicator = new Communicator(client, "Servidor Principal");
        clientCommunicator.set(communicator); // Define o Communicator da thread atual
        boolean exitClient = false;

        try {
            communicator.sendJsonMessage(new ObjectMapper().writeValueAsString(actions.getCommands())); // Envia as opções de menu para o proxy
        } catch (JsonProcessingException e) {
            erro("Erro ao enviar menu ao Cliente: " + e.getMessage());
            exitClient = true;
        }

        try {
            while (running.get() && !exitClient && client.isConnected()) {
                Command option = communicator.receiveJsonMessage(Command.class);

                if (option == DISCONECT) {
                    exitClient = true;
                }

                if (actions.get(option) != null) {
                    actions.get(option).run();
                } else {
                    erro("Cliente enviou uma opção inválida para o ServerMain!");
                }
            }
        } catch (Exception e) {
            erro("Erro com a conexão do Cliente: " + e.getMessage());
        }

        try {
            if (!client.isClosed()) {
                client.close();
            }
            if (clientCommunicator.get() != null) {
                clientCommunicator.get().disconnect();
                clientCommunicator.remove();
            }
        } catch (IOException e) {
            erro("Erro ao fechar conexão com o cliente: " + e);
        }
    }

    public void searchOS(Communicator communicator) {
        OrderService order = communicator.receiveJsonMessage(OrderService.class);
        try {
            order = treeAVL.search(order.getCode());
            communicator.sendJsonMessage(order);
            info("Os dados foram encontrados e enviados!");
        } catch (NodeNotFoundException e) {
            warn("Os dados não foram encontrados na base de dados!");
            communicator.sendJsonMessage(new OrderService());
        }
    }

    public void registerOS(Communicator communicator) {
        OrderService data = communicator.receiveJsonMessage(OrderService.class);
        try {
            synchronized (treeLock) { // Sincroniza o acesso à árvore que pode gerar inconsistência
                treeAVL.insert(data);
            }
            communicator.sendTextMessage("Dado cadastrado com sucesso!");
        } catch (NodeAlreadyExistsException e) {
            communicator.sendTextMessage("Dado já existe na árvore!");
        }
    }

    public void listOS(Communicator communicator) {
        List<OrderService> found = treeAVL.list();

        try {
            communicator.sendJsonMessage(JsonSerializable.objectMapper.writeValueAsString(found));
        } catch (JsonProcessingException e) {
            erro("Erro ao enviar lista para cliente (SERVER): " + e.getMessage());
            disconnectProgram(communicator);
        }
    }

    public void updateOS(Communicator communicator) {
        OrderService data = communicator.receiveJsonMessage(OrderService.class);
        try {
            synchronized (treeLock) { // Sincroniza o acesso à árvore que pode gerar inconsistência
                treeAVL.alter(data);
            }
            communicator.sendTextMessage("Dado alterado com sucesso!");
        } catch (NodeNotFoundException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore!");
        }
    }

    public void removeOS(Communicator communicator) {
        int code = Integer.parseInt(communicator.receiveTextMessage());
        try {
            synchronized (treeLock) { // Sincroniza o acesso à árvore que pode gerar inconsistência
                treeAVL.remove(code);
            }
            communicator.sendTextMessage("Dado removido com sucesso!");
        } catch (InvalidOperationException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore ou já removido!");
        }
    }

    public void quantityRecords(Communicator communicator) {
        communicator.sendTextMessage(String.valueOf(treeAVL.getQuantityRecords()));
    }

    public void disconnectProgram(Communicator communicator) {
        if (communicator != null) {
            communicator.disconnect();
        }
    }

    private void startCommandListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("stop")) {
                    Server.this.stopServer();
                    break;
                }
            }
        }).start();
    }

    public void stopServer() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                info("Servidor ServerMain encerrado.");
            }
        } catch (IOException e) {
            erro("Erro ao fechar o servidor" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}