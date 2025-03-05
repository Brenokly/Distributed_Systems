package org.example.serverproxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.utils.*;
import org.example.utils.common.Cache;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.utils.Command.*;

public class Proxy implements Loggable, JsonSerializable {
    private final int port;                                                                 // Porta do servidor
    private final Menu actions;                                                             // Menu de ações do servidor
    private volatile Cache cache;                                                           // Cache de dados
    Authenticator authenticator;                                                            // Autenticador
    private ServerSocket serverSocket;                                                      // Socket do servidor
    private final ProxyInfo serverInfo;                                                     // Informações do servidor principal
    private final Object cacheLock = new Object();                                          // Lock para sincronização da cache
    private final List<Command> commands = new ArrayList<>();                               // Lista de comandos
    private volatile AtomicBoolean running = new AtomicBoolean(true);              // Flag de controle de exec
    private static final ThreadLocal<Communicator> cliCommunicator = new ThreadLocal<>();   // Comunicador do cliente
    private static final ThreadLocal<Communicator> serCommunicator = new ThreadLocal<>();   // Comunicador do servidor

    public Proxy() {
        this.port = 15552;
        cache = new Cache();
        //this.proxyInfo = new ProxyInfo("26.97.230.179", 15553);
        this.serverInfo = new ProxyInfo("localhost", 15553);
        this.actions = new Menu();
        this.authenticator = new Authenticator("src/main/java/org/example/serverproxy/credenciais.txt");
        initializeDefaultActions();
        createServerSocket();
    }

    private void initializeDefaultActions() {
        commands.add(AUTHENTICATE);
        commands.add(DISCONECT);

        actions.put(SEARCH, () -> searchOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(REGISTER, () -> registerOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(LIST, () -> listOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(UPDATE, () -> updateOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(REMOVE, () -> removeOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(QUANTITY, () -> quantityRecords(cliCommunicator.get(), serCommunicator.get()));
        actions.put(DISCONECT, () -> disconnectProgram(cliCommunicator.get(), serCommunicator.get()));
        actions.put(AUTHENTICATE, () -> authenticate(cliCommunicator.get()));
    }

    private void createServerSocket() {
        try {
            // serverSocket = new ServerSocket(port, 50, InetAddress.getByName("26.97.230.179")); // RemoteHost
            serverSocket = new ServerSocket(port); // LocalHost
            info("Servidor Proxy rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar o Servidor Proxy");

            while (running.get()) {
                try {
                    info("Servidor Proxy Aguardando conexão de um Cliente...");
                    startCommandListener();
                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    erro("Erro ao tentar conectar com o Cliente: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor Proxy!");
            throw new RuntimeException("Erro ao tentar criar o Servidor Proxy!", e);
        } finally {
            stopServer();
        }
    }

    private void handleClient(Socket client) {
        Communicator clientcommunicator = new Communicator(client, "Proxy");
        cliCommunicator.set(clientcommunicator);
        boolean exitClient = false;

        // Envia as opções iniciais do proxy (Autentica e Desconectar)
        try {
            clientcommunicator.sendJsonMessage(new ObjectMapper().writeValueAsString(commands));
        } catch (JsonProcessingException e) {
            erro("Erro ao enviar menu ao Cliente: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // Intermediação de mensagens entre o cliente e o servidor principal
        while (running.get() && !exitClient && client.isConnected()) {
            Command option = clientcommunicator.receiveJsonMessage(Command.class);

            if (option == DISCONECT) {
                exitClient = true;
            }

            if (actions.get(option) != null) {
                actions.get(option).run();
            } else {
                erro("Cliente enviou uma opção inválida para o Proxy!");
            }
        }

        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
            if (serCommunicator != null) {
                if (serCommunicator.get().isConnected()) serCommunicator.get().disconnect();
            }
        } catch (IOException e) {
            erro("Erro ao fechar conexão com o cliente: " + e);
        } finally {
            cliCommunicator.remove();
            if (serCommunicator != null) serCommunicator.remove();
        }
    }

    private void authenticate(Communicator clientcommunicator) {
        boolean authenticated = false;
        int attempts = 0;

        // Autenticação do cliente
        while (running.get() && !authenticated) {
            User clientCredentials = clientcommunicator.receiveJsonMessage(User.class);

            authenticated = authenticator.authenticate(clientCredentials.getLogin(), clientCredentials.getPassword());

            attempts++;

            if (authenticated) {
                clientcommunicator.sendJsonMessage(SUCCESS);
            } else {
                clientcommunicator.sendJsonMessage(INVALID);
                if (attempts == 3) {
                    warn("Número máximo de tentativas de autenticação excedido!");
                    clientcommunicator.sendJsonMessage(ERROR);
                    clientcommunicator.disconnect();
                    return;
                }
            }
        }

        // Conexão com o servidor principal
        Communicator serverCommunicator = new Communicator("Proxy");
        serverCommunicator.connect(serverInfo.getHost(), serverInfo.getPort());
        serCommunicator.set(serverCommunicator);

        // Recebe as opções do servidor principal
        List<Command> serverCommands = JsonSerializable.fromJson(serverCommunicator.receiveTextMessage(), new TypeReference<>() {
        });

        // Envia as opções do servidor principal para o cliente
        try {
            clientcommunicator.sendJsonMessage(new ObjectMapper().writeValueAsString(serverCommands));
        } catch (JsonProcessingException e) {
            erro("Erro ao enviar menu do Proxy ao Cliente: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void disconnectProgram(Communicator clientcommunicator, Communicator servecommunicator) {
        if (clientcommunicator != null) {
            clientcommunicator.disconnect();
        }

        servecommunicator.sendJsonMessage(DISCONECT);
        servecommunicator.disconnect();
    }

    private void quantityRecords(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(QUANTITY);

        int response = Integer.parseInt(servecommunicator.receiveTextMessage());
        clientcommunicator.sendTextMessage(String.valueOf(response));
    }

    private void removeOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(REMOVE); // Envia a ação de remoção para o servidor principal

        int code = Integer.parseInt(clientcommunicator.receiveTextMessage());

        synchronized (cacheLock) {
            cache.remove(code);
        }

        cache.show();

        servecommunicator.sendTextMessage(Integer.toString(code)); // Recebe o ID do cliente e envia para o servidor principal

        clientcommunicator.sendTextMessage(servecommunicator.receiveTextMessage()); // Recebe a confirmação de remoção e envia para o cliente
    }

    private void updateOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(UPDATE); // Envia a ação de atualização para o servidor principal

        OrderService os = clientcommunicator.receiveJsonMessage(OrderService.class); // Recebe do cliente

        synchronized (cacheLock) { // Atualiza na cache
            cache.alter(os);
        }

        cache.show();

        servecommunicator.sendJsonMessage(os); // Envia os dados dá OS para o servidor principal

        clientcommunicator.sendTextMessage(servecommunicator.receiveTextMessage()); // Recebe a confirmação de atualização e envia para o cliente
    }

    private void listOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(LIST);

        List<OrderService> osList = JsonSerializable.fromJson(servecommunicator.receiveTextMessage(), new TypeReference<>() {
        });

        try {
            clientcommunicator.sendJsonMessage(objectMapper.writeValueAsString(osList));
        } catch (JsonProcessingException e) {
            clientcommunicator.sendJsonMessage("Erro ao enviar lista de ordenes!");
            erro("Erro ao enviar lista de ordens de serviço (PROXY)!");
            throw new RuntimeException(e);
        }
    }

    private void registerOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(REGISTER); // Envia a ação de cadastro para o servidor principal

        servecommunicator.sendJsonMessage(clientcommunicator.receiveJsonMessage(OrderService.class)); // Recebe do cliente & Envia para o server

        clientcommunicator.sendTextMessage(servecommunicator.receiveTextMessage()); // Recebe a confirmação de cadastro e envia para o cliente
    }

    private void searchOS(Communicator clientcommunicator, Communicator servecommunicator) {
        OrderService os = clientcommunicator.receiveJsonMessage(OrderService.class);

        OrderService osCache = cache.search(os.getCode());

        if (osCache == null) {
            servecommunicator.sendJsonMessage(SEARCH); // Envia a ação de busca para o servidor principal

            servecommunicator.sendJsonMessage(os); // Envia para o server

            osCache = servecommunicator.receiveJsonMessage(OrderService.class); // Recebe do server

            synchronized (cacheLock) { // Insiro na cache para futuras consultas
                cache.insert(osCache);
            }
        }

        cache.show();

        clientcommunicator.sendJsonMessage(osCache); // Envia para o cliente
    }

    private void startCommandListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("stop")) {
                    stopServer();
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
                info("Servidor Proxy encerrado.");
            }
        } catch (IOException e) {
            erro("Erro ao fechar o servidor" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Proxy();
    }
}