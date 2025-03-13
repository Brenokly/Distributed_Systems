package org.example.serverproxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.utils.*;
import org.example.utils.common.Cache;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;
import org.example.utils.common.RMICommon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.utils.Command.*;

public class Proxy implements Loggable, JsonSerializable, ProxyCacheService, RMICommon {
    private static int proxysCount = 0;                                                     // Contador de instâncias de Proxy
    private static List<Integer> ports = new ArrayList<>();                                 // Lista de portas
    private final int id;                                                                   // Identificador do Proxy
    private final String name;                                                              // Nome do Proxy
    private final int port;                                                                 // Porta do servidor
    private final String host;                                                              // Host do servidor
    private final int portRMI;                                                              // Porta do RMI
    private final Menu actions;                                                             // Menu de ações do servidor
    Authenticator authenticator;                                                            // Autenticador
    private volatile Cache cache;                                                           // Cache de dados
    private ServerSocket serverSocket;                                                      // Socket do servidor
    private final ProxyInfo serverInfo;                                                     // Informações do servidor principal
    private static final Object cacheLock = new Object();                                   // Lock para sincronização da cache
    private final List<Command> commands = new ArrayList<>();                               // Lista de comandos
    private volatile AtomicBoolean running = new AtomicBoolean(false);          // Flag de controle de exec
    private static final ThreadLocal<Communicator> cliCommunicator = new ThreadLocal<>();   // Comunicador do cliente
    private static final ThreadLocal<Communicator> serCommunicator = new ThreadLocal<>();   // Comunicador do servidor

    public Proxy(int port, int portRMI) {
        this.id = proxysCount++;
        name = "Proxy" + id;
        this.port = port;
        this.portRMI = portRMI;
        //this.host = "localhost"; // Para rodar localmente
        this.host = "26.137.178.91"; // Para rodar no servidor
        ports.add(this.portRMI);
        cache = new Cache();
        this.actions = new Menu();
        this.authenticator = new Authenticator();
        this.serverInfo = new ProxyInfo("26.97.230.179", port);
        System.setProperty("java.rmi.server.hostname", "26.97.230.179");
        if (createRmiMethods()) {
            initializeDefaultActions();
            createServerSocket();
        } else {
            erro("Erro ao tentar criar o Proxy " + name + "! (RMI)");
        }
    }

    private boolean createRmiMethods() {
        for (int i = 0; i < 3; i++) {
            try {
                Registry registry = LocateRegistry.createRegistry(portRMI);
                registry.rebind("ProxyCacheService", this);
                info(name + " registrado no RMI...");
                return true;
            } catch (Exception e) {
                erro("Erro ao tentar registrar o Proxy no RMI: Tentativa " + (i + 1) + " de 3");
            }
        }

        return false;
    }

    private void initializeDefaultActions() {
        commands.add(AUTHENTICATE);
        commands.add(DISCONECT);
        actions.put(SEARCH, ()      -> searchOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(REGISTER, ()    -> registerOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(LIST, ()        -> listOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(UPDATE, ()      -> updateOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(REMOVE, ()      -> removeOS(cliCommunicator.get(), serCommunicator.get()));
        actions.put(QUANTITY, ()    -> quantityRecords(cliCommunicator.get(), serCommunicator.get()));
        actions.put(DISCONECT, ()   -> clearSpacesAndDisconnect());
        actions.put(AUTHENTICATE,() -> authenticate(cliCommunicator.get()));
    }

    private void createServerSocket() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host)); 

            running.set(true);

            info("Servidor" + name + " rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar...");

            while (running.get()) {
                try {
                    info("Servidor " + name + " Aguardando conexão de um Cliente...");
                    startCommandListener();

                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    erro("Erro ao tentar conectar com o Cliente: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor " + name + ": " + e);
            throw new RuntimeException("Erro ao tentar criar o Servidor Proxy!", e);
        } finally {
            stopServer();
        }
    }

    private void handleClient(Socket client) {
        if (client == null) {
            erro("Erro ao tentar conectar com o Cliente: Socket nulo!");
            clearSpacesAndDisconnect();
        }

        Communicator clientcommunicator = new Communicator(client,  name + " & Cliente");
        cliCommunicator.set(clientcommunicator);
        
        try {
            sendMenuProxy(clientcommunicator);

            // Intermediação de mensagens entre o cliente e o servidor principal
            while (running.get() && clientcommunicator.isConnected()) {
                Command option = clientcommunicator.receiveJsonMessage(Command.class);

                if (actions.get(option) != null) {
                    if (option == AUTHENTICATE || option == DISCONECT) {
                        actions.get(option).run();
                    } else if (serCommunicator.get() != null && serCommunicator.get().isConnected()){
                        actions.get(option).run();
                    } else {
                        erro("Servidor Principal não está conectado!");
                        clearSpacesAndDisconnect();
                    }
                } else {
                    erro("Cliente enviou uma opção inválida para o " + name);
                }
            }
            // Caso o cliente se desconecte abruptamente preciso capturar a exceção
        } catch (Exception e) {
            erro("Erro ao tentar intermediar a comunicação entre o Cliente e o Servidor Principal: " + e.getMessage());
        } finally {
            clearSpacesAndDisconnect();
        }
    }

    private void clearSpacesAndDisconnect() {
        try {
            if (cliCommunicator.get() != null) {
                if (cliCommunicator.get().isConnected()) {
                    cliCommunicator.get().disconnect();
                }
                cliCommunicator.remove();
            }
            if (serCommunicator.get() != null) {
                if (serCommunicator.get().isConnected()) {
                    serCommunicator.get().sendJsonMessage(DISCONECT);
                    serCommunicator.get().disconnect();
                }
                serCommunicator.remove();
            }

        } catch (Exception e) {
            erro("Proxy: Erro ao fechar/limpar conexão com o cliente: " + e);
        }
    }

    private void sendMenuProxy(Communicator clientcommunicator){
        for (int i = 0; i < 3; i++) {
            try {
                // tenta enviar as opções iniciais do proxy 3 vezes (Autentica e Desconectar)
                clientcommunicator.sendJsonMessage(new ObjectMapper().writeValueAsString(commands));
                return;
            } catch (JsonProcessingException e) {
                erro("Erro ao tentar enviar menu do Proxy ao Cliente: Tentativa " + (i + 1) + " de 3");
            }
        }

        erro("Erro ao enviar menu do " + name + " ao Cliente! Número máximo de tentativas excedido!");
        clearSpacesAndDisconnect();
    }

    private void authenticate(Communicator clientcommunicator) {
        boolean authenticated = false;
        int attempts = 1;

        // Autenticação do cliente
        while (running.get() && attempts <= 3) {
            User clientCredentials = clientcommunicator.receiveJsonMessage(User.class);

            authenticated = authenticator.authenticate(clientCredentials.getLogin(), clientCredentials.getPassword());

            attempts++;

            if (authenticated) {
                clientcommunicator.sendJsonMessage(SUCCESS);
                break;
            } else {
                if (attempts == 4) {
                    warn("Número máximo de tentativas de autenticação excedido!");
                    clientcommunicator.sendJsonMessage(ERROR);
                    clientcommunicator.disconnect();
                    return;
                }
                else {
                    clientcommunicator.sendJsonMessage(INVALID);
                }
            }
        }
        
        // Se autenticado, conecta com o servidor principal
        if (authenticated) {
            conectServer(clientcommunicator);
        } else {
            clearSpacesAndDisconnect();
        }
    }

    private void conectServer(Communicator clientcommunicator){
        // Cria conexão com o servidor principal
        Communicator serverCommunicator = new Communicator( name +  " & Servidor");
        serverCommunicator.connect(serverInfo.getHost(), serverInfo.getPort());
        serCommunicator.set(serverCommunicator);

        // Recebe menu do servidor principal
        List<Command> serverCommands = JsonSerializable.fromJson(serverCommunicator.receiveTextMessage(), new TypeReference<>() {});;

        if (serverCommands == null) {
            erro("Erro ao receber/enviar menu do Servidor Principal!");
            clearSpacesAndDisconnect();
            return;
        }

        for (int i = 0; i < 3; i++) {
            try {
                if (serverCommands != null) {
                    // Tenta envia mensagem para o cliente
                    clientcommunicator.sendJsonMessage(new ObjectMapper().writeValueAsString(serverCommands));
                    break;
                }
            } catch (JsonProcessingException e) {
                erro("Erro ao receber/enviar menu ao Servidor Principal: Tentativa " + (i + 1) + " de 3");
            }
        }               
    }

    private void quantityRecords(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(QUANTITY);

        int response = Integer.parseInt(servecommunicator.receiveTextMessage());
        clientcommunicator.sendTextMessage(String.valueOf(response));
    }

    private void removeOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(REMOVE); // Envia a ação de remoção para o servidor principal

        int code = Integer.parseInt(clientcommunicator.receiveTextMessage()); // Recebe do cliente

        removeCacheSync(code); // Remove da cache

        for (Integer porta : ports) { // Avisa a todos os proxies para remover essa ordem
            if (porta.intValue() != this.port) { 
                try {
                    // Localiza o registro RMI no IP e porta do servidor
                    Registry registry = LocateRegistry.getRegistry(host, porta);
    
                    // Obtém a referência do objeto remoto pelo nome
                    ProxyCacheService servico = (ProxyCacheService) registry.lookup("ProxyCacheService");
                
                    servico.removePOS(code);
                } catch (Exception e) {
                    erro("Erro ao tentar contatar o proxy na porta: " + porta + ": " + e.getMessage());
                    continue;
                } 
            }
        }

        cache.show();

        servecommunicator.sendTextMessage(Integer.toString(code)); // Recebe o ID do cliente e envia para o servidor principal

        clientcommunicator.sendTextMessage(servecommunicator.receiveTextMessage()); // Recebe a confirmação de remoção e envia para o cliente
    }

    private void updateOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(UPDATE); // Envia a ação de atualização para o servidor principal

        OrderService os = clientcommunicator.receiveJsonMessage(OrderService.class); // Recebe do cliente

        updateCacheSync(os); // Atualiza na cache

        for (Integer porta : ports) { // Avisa a todos os proxies para alterar essa ordem
            if (porta.intValue() != this.port) { 
                try {
                    // Localiza o registro RMI no IP e porta do servidor
                    Registry registry = LocateRegistry.getRegistry(host, porta);
    
                    // Obtém a referência do objeto remoto pelo nome
                    ProxyCacheService servico = (ProxyCacheService) registry.lookup("ProxyCacheService");
                
                    servico.updatePOS(os);
                } catch (Exception e) {
                    erro("Erro ao tentar contatar o proxy na porta: " + porta + ": " + e.getMessage());
                    continue;
                } 
            }
        }

        cache.show();

        servecommunicator.sendJsonMessage(os); // Envia os dados dá OS para o servidor principal

        clientcommunicator.sendTextMessage(servecommunicator.receiveTextMessage()); // Recebe a confirmação de atualização e envia para o cliente
    }

    private void listOS(Communicator clientcommunicator, Communicator servecommunicator) {
        servecommunicator.sendJsonMessage(LIST); // Envia a ação de listagem para o servidor principal

        List<OrderService> osList = JsonSerializable.fromJson(servecommunicator.receiveTextMessage(), new TypeReference<>() {
        }); // Recebe do server

        try {
            clientcommunicator.sendJsonMessage(objectMapper.writeValueAsString(osList)); // Envia para o cliente
        } catch (JsonProcessingException e) {
            clientcommunicator.sendJsonMessage(new ArrayList<OrderService>()); // Envia lista vazia
            erro("Erro ao enviar lista de ordens de serviço (" + name + ")!");
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

        if (cache == null) {
            for (Integer porta : ports) { // Busca em nas caches dos outros proxies
                if (porta.intValue() != this.port) { 
                    try {
                        // Localiza o registro RMI no IP e porta do servidor
                        Registry registry = LocateRegistry.getRegistry(host, porta);
        
                        // Obtém a referência do objeto remoto pelo nome
                        ProxyCacheService servico = (ProxyCacheService) registry.lookup("ProxyCacheService");
                    
                        osCache = servico.searchPOS(os.getCode());

                        if (osCache != null) {
                            break;
                        }
                    } catch (Exception e) {
                        erro("Erro ao tentar contatar o proxy na porta: " + porta + ": " + e.getMessage());
                        continue;
                    } 
                }
            }

            if (osCache == null) { // Busca no servidor principal
                servecommunicator.sendJsonMessage(SEARCH); // Envia a ação de busca para o servidor principal

                servecommunicator.sendJsonMessage(os); // Envia para o server
    
                osCache = servecommunicator.receiveJsonMessage(OrderService.class); // Recebe do server
            }
            
            if (osCache != null) { 
                insertCacheSync(osCache); // Insere na cache
            }
        }

        cache.show();

        clientcommunicator.sendJsonMessage(osCache);    // Envia para o cliente
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
            scanner.close();
        }).start();
    }

    public void stopServer() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                info("Servidor " + name + " encerrado.");
            }
        } catch (IOException e) {
            erro("Erro ao fechar o servidor " + name + e.getMessage());
        }
    }

    // Novos Métodos RMI's

    @Override
    public boolean updatePOS(OrderService value) throws RemoteException {
        return updateCacheSync(value);
    }

    @Override
    public boolean removePOS(int code) throws RemoteException {
        return removeCacheSync(code);
    }

    @Override
    public OrderService searchPOS(int code) throws RemoteException {
        return cache.search(code);
    }

    // Métodos syncronos

    private boolean removeCacheSync(int code) {
        synchronized (cacheLock) {
            return cache.remove(code);
        }
    }

    private boolean updateCacheSync(OrderService value) {
        synchronized (cacheLock) {
            return cache.alter(value);
        }
    }

    private void insertCacheSync(OrderService value) {
        synchronized (cacheLock) {
            cache.insert(value);
        }
    }

    @Override
    public boolean isRunning() throws RemoteException {
        return running.get();
    }

    public static void main(String[] args) {
        new Proxy(15550, 15551);
    }
}