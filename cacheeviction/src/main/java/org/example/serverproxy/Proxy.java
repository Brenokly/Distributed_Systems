package org.example.serverproxy;

/**
 * Classe Proxy
 * 
 * Classe que representa um Proxy de um servidor de cache.
 * Esta classe é responsável por intermediar a comunicação entre o cliente e o servidor principal.
 * 
 * Interface ProxyCacheService
 * 1 - Loggable: Interface para exibição de mensagens de log
 * 2 - JsonSerializable: Interface para serialização e deserialização de objetos JSON
 * 3 - ProxyCacheService: Interface para métodos de cache RMI
 * 4 - RMICommon: Interface para métodos RMI comuns
 * 
 * A respeito da "sincronizaçaõ entre as caches":
 * 
 * 1 - Alteração e Remoção: Quando essas operações acontecem, elas são propagadas para todas as caches dos outros proxies ativos.
 * é como se fosse um "broadcast" para todos os outros proxies / o proxy que fez a alteração fosse momentaneamente o líder.
 * 
 * 2 - Busca: Quando um proxy não encontra um registro na sua cache, ele busca nas caches dos outros proxies ativos 
 * e no servidor principal.
 * 
 * @version 1.0
*/

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.locator.LocalizerInterface;
import org.example.utils.Command;
import org.example.utils.JsonSerializable;
import org.example.utils.LockCache;
import org.example.utils.Loggable;
import org.example.utils.Menu;
import org.example.utils.ProxyInfo;
import org.example.utils.User;
import org.example.utils.common.Cache;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;
import org.example.utils.common.RMICommon;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.example.utils.Command.*;

public class Proxy implements Loggable, JsonSerializable, ProxyService, RMICommon {
    private final Logger logger;                                                            // Logger personalizado
    private final int id;                                                                   // Identificador do Proxy
    private final int port;                                                                 // Porta do servidor
    private final String name;                                                              // Nome do Proxy
    private String host;                                                                    // Host dos servidores
    private final int portRMI;                                                              // Porta do RMI do proxy
    private final Menu actions;                                                             // Menu de ações do servidor
    Authenticator authenticator;                                                            // Autenticador
    private volatile Cache cache;                                                           // Cache de dados
    private ServerSocket serverSocket;                                                      // Socket do servidor
    private final ProxyInfo serverInfo;                                                     // (Id/Porta) do servidor principal
    private static final List<Integer> ports = new ArrayList<>();                           // Lista de portasRMI dos proxys
    private static final LockCache cacheLock = new LockCache();                             // Lock para sincronização da cache
    private final List<Command> commands = new ArrayList<>();                               // Lista de comandos
    private volatile AtomicBoolean running = new AtomicBoolean(true);          // Flag de controle de exec
    private AtomicInteger clients = new AtomicInteger(0);                      // Número de clientes conectados
    private static final ThreadLocal<Communicator> cliCommunicator = new ThreadLocal<>();   // Comunicador do cliente por Thread
    private static final ThreadLocal<Communicator> serCommunicator = new ThreadLocal<>();   // Comunicador do servidor por Thread

    private final int finderPortRMI = 14442;                     // Porta RMI do Localizador

    public Proxy(int port, int portRMI, String loggerName, int id) {
        this.logger = LoggerFactory.getLogger(loggerName);
        this.id = id;
        this.name = "Proxy" + id;
        clearLog(name);
        this.port = port;
        this.portRMI = portRMI;
        
        // Defina o host baseado em configuração
        try {
            this.host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            erro("Erro ao obter o endereço IP do host: " + e.getMessage());
            this.host = "26.137.178.91";
        }
        
        this.cache = new Cache();
        this.actions = new Menu();
        this.authenticator = new Authenticator();
        this.serverInfo = new ProxyInfo("ServidorMain", host, 16660, 15000);
        
        configurarRMI();

        if (createRmiMethods()) {
            notifyFinder();
            initializeDefaultActions();
            createServerSocket();
        } else {
            erro("Erro ao tentar criar o " + name + "! (RMI)");
            stopServer();
        }
    }

    private void configurarRMI() {
        System.setProperty("java.rmi.server.hostname", host);
    }

    private boolean createRmiMethods() {
        try {
            // Cria os objetos remotos
            Object proxyMethodsRemote = UnicastRemoteObject.exportObject(this, 0);

            // Cria o registro RMI
            Registry registry = LocateRegistry.createRegistry(portRMI);

            // Registra os objetos remotos no RMI
            registry.bind("ProxyService", (ProxyService) proxyMethodsRemote);
            registry.bind("RMICommon", (RMICommon) proxyMethodsRemote);

            info(name + " registrou seus métodos no RMI...");
            return true;
        } catch (RemoteException e) {
            erro("Erro ao tentar criar os objetos do Proxy" + id + " no RMI: " + e.getMessage());
        } catch (AlreadyBoundException e) {
            erro("Erro ao tentar registrar o Proxy" + id + " no RMI. Já existe um método com o mesmo nome!" + e.getMessage());
        }
    
        return false;
    }    

    private void notifyFinder() {
        Thread thread = new Thread(() -> {
            AtomicBoolean success = new AtomicBoolean(false);
            while (!success.get() && running.get()) {
                try {
                    // Localiza o registro RMI no IP e porta do servidor
                    Registry registry = LocateRegistry.getRegistry(host, finderPortRMI);
        
                    // Obtém a referência do objeto remoto pelo nome
                    LocalizerInterface localizer = (LocalizerInterface) registry.lookup("Localizador");
        
                    // Registra o Proxy no Localizador
                    localizer.registerProxy(name, host, port, portRMI);
        
                    info(name + " registrado no Localizador...");
                    success.set(true);
                } catch (Exception e) {
                    erro("Erro ao tentar notificar o Localizador: Servidor Localizador não encontrado no momento!");
                    try {
                        Thread.sleep(2000); // Tenta novamente a cada 2 segundos
                    } catch (InterruptedException ex) {
                        erro("Erro ao tentar dormir a Thread: " + ex.getMessage());
                    }
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
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
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) { 
            info("Servidor " + name + " rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar!");
            this.serverSocket = serverSocket;
            running.set(true);

            // Verificando se o usuário deseja encerrar o servidor          
            startCommandListener();
            info("Servidor " + name + " Aguardando conexão de um Cliente...");

            while (running.get()) {
                try {
                    // Aguarda a conexão de um cliente
                    Socket client = serverSocket.accept();

                    // Incrementa o número de clientes conectados
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
            erro("Erro ao tentar conectar com o Cliente: Socket nulo!");
            clearSpacesAndDisconnect();
        }

        Communicator clientcommunicator = new Communicator(client,  name + " & Cliente");
        cliCommunicator.set(clientcommunicator);
        String clientIp = clientcommunicator.getSocket().getInetAddress().getHostAddress();

        try {
            // Verifica se o cliente está autenticado
            if (authenticator.isAuthenticated(clientIp)) {
                conectServer(clientcommunicator);
            } else {
                sendMenuProxy(clientcommunicator);
            }

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
            clients.decrementAndGet();
            clearSpacesAndDisconnect();
        }
    }

    private void clearSpacesAndDisconnect() {
        try {
            if (cliCommunicator.get() != null && cliCommunicator.get().getSocket() != null) {
                if (cliCommunicator.get().isConnected()) {
                    cliCommunicator.get().disconnect();
                }
                cliCommunicator.remove();
            }
            if (serCommunicator.get() != null && serCommunicator.get().getSocket() != null) {
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

            authenticated = authenticator.authenticate(clientCredentials.getLogin(), clientCredentials.getPassword(), 
                                            clientcommunicator.getSocket().getInetAddress().getHostAddress());

            for (Integer porta : ports) { // Avisa a todos os proxies para remover essa ordem
                if (porta.intValue() != this.port) { 
                    try {
                        // Localiza o registro RMI no IP e porta do servidor
                        Registry registry = LocateRegistry.getRegistry(host, porta);
        
                        // Obtém a referência do objeto remoto pelo nome
                        ProxyService servico = (ProxyService) registry.lookup("ProxyService");
                    
                        servico.authenticateClient(clientCredentials.getLogin(), clientCredentials.getPassword(), 
                        clientcommunicator.getSocket().getInetAddress().getHostAddress());
                    } catch (Exception e) {
                        erro("Erro ao tentar contatar o proxy da porta: " + porta + ": " + e.getMessage());
                        continue;
                    } 
                }
            }

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

    public void authenticateClient(String user, String password, String ip) throws RemoteException {
        authenticator.authenticate(user, password, ip);
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
                    ProxyService servico = (ProxyService) registry.lookup("ProxyService");
                
                    servico.removePOS(code);
                } catch (Exception e) {
                    erro("Erro ao tentar contatar o proxy da porta: " + porta + ": " + e.getMessage());
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
                    ProxyService servico = (ProxyService) registry.lookup("ProxyService");
                
                    servico.updatePOS(os);
                } catch (Exception e) {
                    erro("Erro ao tentar contatar o proxy da porta: " + porta + ": " + e.getMessage());
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
        // Recebe a ordem de serviço do cliente
        OrderService os = clientcommunicator.receiveJsonMessage(OrderService.class); 

        // Busca na cache
        OrderService osCache = cache.search(os.getCode());

        // Se não encontrou na cache, busca nos outros proxies e no servidor principal
        if (osCache == null) {
            if (ports.isEmpty()) {
                info("Elemento não foi encontrada na própria cache e não há outros proxies ativos para buscar a ordem de serviço!");
            } else {
                info("Elemento não foi encontrada na própria cache. Buscando em outras caches...");
                for (Integer porta : ports) { // Busca nas caches dos outros proxies
                    if (porta.intValue() != this.portRMI) { 
                        try {
                            // Localiza o registro RMI no IP e porta do servidor
                            Registry registry = LocateRegistry.getRegistry(host, porta.intValue());
            
                            // Obtém a referência do objeto remoto pelo nome
                            ProxyService servico = (ProxyService) registry.lookup("ProxyService");
                        
                            osCache = servico.searchPOS(os.getCode());

                            if (osCache != null) {
                                info("OS encontrada na cache do Proxy da porta: " + porta);
                                break;
                            }
                        } catch (Exception e) {
                            erro("Erro ao tentar contatar o proxy da porta: " + porta + ": " + e.getMessage());
                            continue;
                        } 
                    }
                }
            }

            // Se não encontrou nas caches dos outros proxies, busca no servidor principal
            if (osCache == null) { 
                info("OS não encontrada em nenhuma cache! Buscando no servidor principal...");
                
                servecommunicator.sendJsonMessage(SEARCH); // Envia a ação de busca para o servidor principal

                servecommunicator.sendJsonMessage(os);  // Envia para o server
    
                osCache = servecommunicator.receiveJsonMessage(OrderService.class); // Recebe do server
            }
            
            if (osCache != null) { 
                insertCacheSync(osCache); // Insere na cache
            }
        }

        cache.show();

        clientcommunicator.sendJsonMessage(osCache); // Envia para o cliente
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

    // Métodos RMI's

    @Override
    public boolean updatePOS(OrderService value) throws RemoteException {
        Boolean bool = updateCacheSync(value);
        cache.show();
        return bool;
    }

    @Override
    public boolean removePOS(int code) throws RemoteException {
        Boolean bool = removeCacheSync(code);
        cache.show();
        return bool;
    }

    @Override
    public OrderService searchPOS(int code) throws RemoteException {
        return cache.search(code);
    }

    @Override
    public boolean isRunning() throws RemoteException {
        return running.get();
    }

    @Override
    public int numberOfClients() throws RemoteException {
        return clients.get();
    }

    @Override
    public boolean insertPortRmi(List<Integer> portRmi) throws RemoteException {
        if (portRmi != null && !portRmi.isEmpty()) {
            ports.clear();
            ports.addAll(portRmi);
            return true;
        }
        return false;
    }

    @Override
    public int incrementClients() throws RemoteException {
        return clients.incrementAndGet();
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
    public Logger logger() {
        return this.logger;
    }
}