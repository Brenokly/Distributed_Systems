package org.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.utils.Command;
import org.example.utils.JsonSerializable;
import org.example.utils.Loggable;
import org.example.utils.Menu;
import org.example.utils.ServerLock;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;
import org.example.utils.exceptions.InvalidOperationException;
import org.example.utils.exceptions.NodeAlreadyExistsException;
import org.example.utils.exceptions.NodeNotFoundException;
import org.example.utils.tree.TreeAVL;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.example.utils.Command.*;

public class Server implements Loggable, JsonSerializable {
    private final int port;                                                                     // Porta do servidor
    private final String host;                                                                  // Host do servidor
    private final Menu actions;                                                                 // Menu de ações do servidor
    private volatile TreeAVL treeAVL;                                                           // Árvore AVL de dados
    private ServerSocket serverSocket;                                                          // Socket do servidor
    private volatile AtomicBoolean running = new AtomicBoolean(true);              // Flag de controle de execução
    private static final ThreadLocal<Communicator> clientCommunicator = new ThreadLocal<>();    // Comunicador do cliente
    private static final ServerLock lock = new ServerLock();                                    // Lock para sincronização do servidor
    private BackupInterface backupServer;                                                       // Servidor de backup
    private static final int BACKUP_PORT = 13337;                                               // Porta do servidor de backup

    public Server() {
        clearLog();
        this.port = 16660;
        this.host = "26.97.230.179";
        this.treeAVL = new TreeAVL();
        this.actions = new Menu();
        configurarRMI();
        connectToBackup();
        initializerTree();
        initializerDefaultActions();
        createServerSocket();
    }

    private void configurarRMI() {
        System.setProperty("java.rmi.server.hostname", host);
    }

    private void connectToBackup() {
        new Thread(() -> {
            boolean connected = false;
            while (!connected) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host, BACKUP_PORT);
                    this.backupServer = (BackupInterface) registry.lookup("backupServer");
                    info("Conectado ao Servidor de Backup!");
                    connected = true;
                } catch (Exception e) {
                    erro("Falha ao conectar ao Servidor de Backup: Nova tentativa em 5 segundos...");
                    try {
                        Thread.sleep(5000); // Tenta conectar a cada 5 segundos
                    } catch (InterruptedException ex) {
                        erro("Erro ao tentar dormir a thread: " + ex.getMessage());
                    }
                }
            }
            syncWithBackup();
        }).start();
    }

    public void initializerTree() {
        try {
            for (int i = 0; i < 100; i++) {
                OrderService os = new OrderService("Nome" + i, "Descrição" + i);
                treeAVL.insert(os);
            }
        } catch (NodeAlreadyExistsException e) {
            erro("Erro ao inserir dados na árvore AVL: " + e.getMessage());
        }
    }

    private void syncWithBackup() {
        synchronized (lock) { // Trava os métodos de inserção, alteração e remoção para sincronizar
            try {
                if (backupServer != null) {
                    backupServer.syncData(treeAVL.list());
                    info("Dados sincronizados com o Servidor de Backup!");
                } else {
                    erro("Servidor de Backup não conectado, não foi possível sincronizar os dados!");
                }
            } catch (RemoteException e) {
                erro("Erro ao sincronizar dados com o Servidor de Backup: " + e.getMessage());
            }
        }
    }

    private void initializerDefaultActions() {
        actions.put(SEARCH, () -> searchOS(clientCommunicator.get()));
        actions.put(REGISTER, () -> registerOS(clientCommunicator.get()));
        actions.put(LIST, () -> listOS(clientCommunicator.get()));
        actions.put(UPDATE, () -> updateOS(clientCommunicator.get()));
        actions.put(REMOVE, () -> removeOS(clientCommunicator.get()));
        actions.put(QUANTITY, () -> quantityRecords(clientCommunicator.get()));
        actions.put(DISCONECT, () -> clearSpacesAndDisconnect());
    }

    private void createServerSocket() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
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

        Communicator communicator = new Communicator(client, "Servidor X Proxy");
        clientCommunicator.set(communicator); // Define o Communicator da thread atual

        try {
            sendMenuServer(communicator);

            while (running.get() && communicator.isConnected()) {
                Command option = communicator.receiveJsonMessage(Command.class);

                if (actions.get(option) != null) {
                    actions.get(option).run();
                } else {
                    erro("Cliente enviou uma opção inválida para o ServerMain!");
                }
            }
        } catch (Exception e) {
            erro("Erro com a conexão do Cliente: " + e.getMessage());
        } finally {
            clearSpacesAndDisconnect();
        }
    }

    private void clearSpacesAndDisconnect() {
        try {
            if (clientCommunicator.get() != null) {
                if (clientCommunicator.get().isConnected()) {
                    clientCommunicator.get().disconnect();
                }
                clientCommunicator.remove();
            }
        } catch (Exception e) {
            erro("Server: Erro ao fechar/limpar conexão com o cliente: " + e);
        }
    }

    private void sendMenuServer(Communicator communicator) {
        for (int i = 0; i < 3; i++) {
            try {
                // tenta enviar as opções iniciais do proxy 3 vezes (Autentica e Desconectar)
                communicator.sendJsonMessage(new ObjectMapper().writeValueAsString(actions.getCommands()));
                return;
            } catch (JsonProcessingException e) {
                erro("Erro ao tentar enviar menu do Servidor ao Cliente: Tentativa " + (i + 1) + " de 3");
            }
        }

        erro("Erro ao enviar menu do Servidor ao Cliente! Número máximo de tentativas excedido!");
        clearSpacesAndDisconnect();
    }

    public void searchOS(Communicator communicator) {
        OrderService order = communicator.receiveJsonMessage(OrderService.class);
        try {
            order = treeAVL.search(order.getCode());
            communicator.sendJsonMessage(order);
            info("Os dados foram encontrados e enviados: " + order);
        } catch (NodeNotFoundException e) {
            warn("Os dados não foram encontrados na base de dados!");
            communicator.sendJsonMessage(new OrderService());
        }
    }

    public void registerOS(Communicator communicator) {
        OrderService data = communicator.receiveJsonMessage(OrderService.class);
        try {
            inserSync(data);
            communicator.sendTextMessage("Dado cadastrado com sucesso: " + data);
        } catch (NodeAlreadyExistsException e) {
            communicator.sendTextMessage("Dado já existe na árvore: " + data);
        }
    }

    public void listOS(Communicator communicator) {
        List<OrderService> found = treeAVL.list();

        try {
            communicator.sendJsonMessage(JsonSerializable.objectMapper.writeValueAsString(found));
        } catch (JsonProcessingException e) {
            erro("Erro ao enviar lista para cliente (SERVER): " + e.getMessage());
            clearSpacesAndDisconnect();
        }
    }

    public void updateOS(Communicator communicator) {
        OrderService data = communicator.receiveJsonMessage(OrderService.class);
        try {
            alterSync(data);
            communicator.sendTextMessage("Dado alterado com sucesso: " + data);
        } catch (NodeNotFoundException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore: " + data);
        }
    }

    public void removeOS(Communicator communicator) {
        int code = Integer.parseInt(communicator.receiveTextMessage());
        try {
            removeSync(code);
            communicator.sendTextMessage("Dado removido com sucesso!");
        } catch (InvalidOperationException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore ou já removido!");
        }
    }

    public void quantityRecords(Communicator communicator) {
        communicator.sendTextMessage(String.valueOf(treeAVL.getQuantityRecords()));
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
            scanner.close();
        }).start();
    }

    // Métodos sincronizados para evitar inconsistências

    private void inserSync(OrderService order) throws NodeAlreadyExistsException {
        synchronized (lock) {
            treeAVL.insert(order);
            try {
                if (backupServer != null)
                    backupServer.replicateInsert(order); // Replicar inserção
            } catch (RemoteException e) {
                erro("Erro ao replicar inserção: " + order);
            }
        }
    }

    private void alterSync(OrderService order) throws NodeNotFoundException {
        synchronized (lock) {
            treeAVL.alter(order);
            try {
                if (backupServer != null)
                    backupServer.replicateUpdate(order); // Replicar atualização
            } catch (RemoteException e) {
                erro("Erro ao replicar atualização: " + order);
            }
        }
    }

    private void removeSync(int code) throws InvalidOperationException {
        synchronized (lock) {
            treeAVL.remove(code);
            try {
                if (backupServer != null)
                    backupServer.replicateRemove(code); // Replicar remoção
            } catch (RemoteException e) {
                erro("Erro ao replicar remoção: " + code);
            }
        }
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