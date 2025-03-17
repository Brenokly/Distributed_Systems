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
    private static int lastCode = 99;                                                           // Último código de registro

    public Server() {
        clearLog("Server");
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

    public void initializerTree() {
        try {
            for (int i = 0; i < 100; i++) {
                OrderService os = new OrderService(i, "Nome" + i, "Descrição" + i);
                treeAVL.insert(os);
            }
        } catch (NodeAlreadyExistsException e) {
            erro("Erro ao inserir dados na árvore AVL: " + e.getMessage());
        }
    }

    private void connectToBackup() {
        Thread thread = new Thread(() -> {
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
        });

        thread.setDaemon(true);  // Configura a thread como daemon caso o servidor principal seja encerrado
        thread.start();             // Inicia a thread
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
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) {
            info("Servidor Principal rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar o Servidor ServerMain!");
            this.serverSocket = serverSocket;
            running.set(true);

            // Iniciar o listener de comandos para capturar o 'stop'
            startCommandListener();
            info("Servidor Principal Aguardando conexão de um Cliente...");

            while (running.get()) {
                try {
                    // Aguarda a conexão de um cliente
                    Socket client = serverSocket.accept();

                    // Cria uma nova thread para tratar o cliente
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    if (running.get()) 
                        erro("Problema ao tentar conectar com o Cliente");
                }
            }
        } catch (Exception e) {
            if (running.get()) 
                info("Erro ao tentar criar o ServerSocket: " + e.getMessage());
        }
    
        info("Servidor Localizador encerrado!");
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
            if (clientCommunicator.get() != null && clientCommunicator.get().getSocket() != null) {
                info("Limpando e desconectando o " + clientCommunicator.get().getName());
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
                info("Menu do Servidor enviado ao Cliente com sucesso!");
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
        data.setCode(++lastCode);
        try {
            inserSync(data);
            communicator.sendTextMessage("Dado cadastrado com sucesso: " + data);
            info("Dado cadastrado com sucesso: " + data);
        } catch (NodeAlreadyExistsException e) {
            communicator.sendTextMessage("Dado já existe na árvore: " + data);
        }
    }

    public void listOS(Communicator communicator) {
        List<OrderService> found = treeAVL.list();

        try {
            communicator.sendJsonMessage(JsonSerializable.objectMapper.writeValueAsString(found));
            info("Dados enviados com sucesso:\n" + generateTable(treeAVL.list()));
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
            info("Dado alterado com sucesso: " + data);
        } catch (NodeNotFoundException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore: " + data);
        }
    }

    public void removeOS(Communicator communicator) {
        int code = Integer.parseInt(communicator.receiveTextMessage());
        try {
            removeSync(code);
            communicator.sendTextMessage("Dado removido com sucesso!");
            info("Dado removido com sucesso: " + code);
        } catch (InvalidOperationException e) {
            communicator.sendTextMessage("Dado não encontrado na árvore ou já removido!");
        }
    }

    public void quantityRecords(Communicator communicator) {
        communicator.sendTextMessage(String.valueOf(treeAVL.getQuantityRecords()));
        info("Quantidade de registros enviada para o cliente: " + treeAVL.getQuantityRecords());
    }

    private void startCommandListener() {
        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (running.get()) {  
                    String command = scanner.nextLine();
                    if ("stop".equalsIgnoreCase(command)) {
                        stopServer();
                        break;  
                    }
                }
            }
        }).start();
    }  

    public void stopServer() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();  // Fecha o serverSocket, interrompendo o accept
            }
        } catch (IOException e) {
            erro("Erro ao tentar fechar o ServerSocket: " + e.getMessage());
        }
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

    // Método para exibir a lista em formato de tabela
    private String generateTable(List<OrderService> services) {
        if (services.isEmpty()) {
        return "Nenhum serviço encontrado.";
        }

        StringBuilder table = new StringBuilder();

        // Cabeçalho da tabela
        table.append(String.format("%-6s | %-20s | %-30s | %-8s%n", "Código", "Nome", "Descrição", "Hora"));
        table.append("----------------------------------------------------------------------\n");

        // Adicionando os dados da lista
        for (OrderService service : services) {
        table.append(String.format("%-6d | %-20s | %-30s | %-8s%n",
            service.getCode(),
            service.getName(),
            service.getDescription().length() > 30 ? service.getDescription().substring(0, 27) + "..."
                : service.getDescription(),
            service.getRequestTime()));
        }

        return table.toString();
    }

    public static void main(String[] args) {
        new Server();
    }
}