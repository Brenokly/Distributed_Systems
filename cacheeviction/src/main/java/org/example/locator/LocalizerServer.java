package org.example.locator;

import lombok.Data;
import org.example.utils.Loggable;
import org.example.utils.ProxyInfo;
import org.example.utils.common.Communicator;
import org.example.utils.common.RMICommon;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class LocalizerServer implements Loggable, LocalizerInterface {
    private final int port;                                                         // Porta do Servidor Localizador
    private final int portRMI;                                                      // Porta RMI do Servidor Localizador
    private final String host;                                                      // Host do Servidor Localizador
    private ServerSocket serverSocket;                                              // Socket do Servidor Localizador
    private final Map<Integer, RMICommon> rmiCommons = new ConcurrentHashMap<>();   // Mapa para armazenar os métodos dos Proxies
    private final List<ProxyInfo> proxyInfo = new CopyOnWriteArrayList<>();         // Lista para armazenar as informações dos Proxies
    private AtomicBoolean running = new AtomicBoolean(false);          // Flag para controlar o Servidor Localizador

    // Configurações do Servidor Localizador
    // ==========================================
    private final String HOST = "26.97.230.179";
    private final int PORT = 14441;
    private final int PORT_RMI = 14442;
    // ===========================================

    public LocalizerServer() {
        clearLog("LocalizerServer");
        this.host = HOST;
        this.port = PORT;
        this.portRMI = PORT_RMI;
        configurarRMI();
        if (createRMI()) {
            createServerSocket();
        } else {
            erro("Erro ao tentar criar o RMI no Servidor Localizador! Tente novamente...");
        }
    }

    private void configurarRMI() {
        System.setProperty("java.rmi.server.hostname", HOST);
    }

    private boolean createRMI() {
        try {
            LocalizerInterface localizer = (LocalizerInterface) UnicastRemoteObject.exportObject(this, 0);

            Registry registry = LocateRegistry.createRegistry(portRMI);

            registry.bind("Localizador", localizer); // Serviço para os Proxies se registrarem

            info("RMI criado com sucesso na porta: " + PORT_RMI);
            return true;
        } catch (Exception e) {
            erro("Erro ao tentar criar o RMI no Servidor Localizador: " + e.getMessage());
        }

        return false;
    }

    private void getRmiMethods() {
        if (proxyInfo.isEmpty()) {
            erro("Nenhum proxy disponível para obter métodos RMI.");
            return;
        }

        ProxyInfo proxy = proxyInfo.getFirst();
        for (int i = 0; i < 3; i++) {
            try {
                Registry registry = LocateRegistry.getRegistry(host, proxy.getPortRMI());
                rmiCommons.put(proxy.getPort(), (RMICommon) registry.lookup("RMICommon"));
                info("Métodos do " + proxy.getName() + " no RMI registrados com sucesso!");
                providePorts();
                info(formatRMICommons(rmiCommons));
                return;
            } catch (Exception e) {
                erro("Erro ao tentar pegar os métodos do " + proxy.getPort() + " no RMI: Tentativa " + (i + 1)
                        + " de 3");
            }
        }

        erro("Falha ao pegar os métodos do " + proxy.getName() + " após 3 tentativas");
    }

    // Fornece para todos os proxies as portas RMI uns aos outros através do método insertPortRmi
    private void providePorts() {
        proxyCheck();

        if (proxyInfo.size() < 2) {
            erro("Não há proxies suficientes para fornecer as portas RMI uns aos outros!");
            return;
        }

        for (ProxyInfo proxy : proxyInfo) {
            try {
                rmiCommons.get(proxy.getPort()).insertPortRmi(proxyInfo.stream()
                        .filter(p -> p.getPort() != proxy.getPort())
                        .map(ProxyInfo::getPortRMI)
                        .toList());
            } catch (Exception e) {
                erro("Erro ao fornecer a porta RMI " + proxy.getPortRMI() + " para o " + proxy.getName() + ": "
                        + e.getMessage());
            }
        }
        info("Portas RMI fornecidas com sucesso ao novo Proxy!");
    }

    private void createServerSocket() {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST))) {
            info("Servidor Localizador rodando na porta: " + serverSocket.getLocalPort());
            info("Digite 'stop' a qualquer momento para encerrar o Servidor Localizador!");
            this.serverSocket = serverSocket;
            running.set(true);
    
            // Inicia o listener de comandos para capturar o 'stop'
            startCommandListener();
            info("Servidor Aguardando conexão de um Cliente...");

            while (running.get()) {
                try {
                    // Aguardando a conexão de um cliente
                    Socket client = serverSocket.accept();

                    // Inicia um novo thread para lidar com o cliente
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
        Communicator communicator = new Communicator(client, "Localizador");
        try {
            String message = communicator.receiveTextMessage();

            if (message.equals("GET_PROXY")) {
                if (rmiCommons.isEmpty()) {
                    communicator.sendJsonMessage(null);
                } else {
                    communicator.sendJsonMessage(loadBalancing().get());
                }
            } else {
                communicator.sendTextMessage("Mensagem inválida!");
            }

        } catch (Exception e) {
            erro("Erro ao lidar com o cliente: " + e.getMessage());
        } finally {
            clearSpacesAndDisconnect(communicator);
        }
    }

    private void clearSpacesAndDisconnect(Communicator communicator) {
        try {
            if (communicator != null) {
                communicator.disconnect();
            }
        } catch (Exception e) {
           erro("Erro ao fechar conexão com o cliente!");
        }
    }

    // Método para balancer a carga entre os Proxies
    private Optional<ProxyInfo> loadBalancing() {
        proxyCheck();
        int min = Integer.MAX_VALUE;
        ProxyInfo chosenProxy = null;

        for (Map.Entry<Integer, RMICommon> entry : rmiCommons.entrySet()) {
            try {
                int clients = entry.getValue().numberOfClients();
                if (clients < min) {
                    min = clients;
                    chosenProxy = proxyInfo.stream()
                            .filter(p -> p.getPort() == entry.getKey())
                            .findFirst()
                            .orElse(null);
                }
            } catch (Exception e) {
                erro("Proxy " + entry.getKey() + " não está mais disponível! Removendo da lista...");
                rmiCommons.remove(entry.getKey());
                proxyInfo.removeIf(p -> p.getPort() == entry.getKey());
            }
        }

        if (chosenProxy != null) {
            try {
                min = rmiCommons.get(chosenProxy.getPort()).incrementClients();
            } catch (Exception e) {
                erro("Erro ao tentar incrementar o número de clientes no Proxy " + chosenProxy.getName());
            }
            info("Proxy escolhido: " + chosenProxy.getName() + " com " + min + " cliente(s) conectados!");

            info(formatRMICommons(rmiCommons));
        } else {
            erro("Nenhum Proxy disponível para balanceamento de carga.");
        }

        return Optional.ofNullable(chosenProxy);
    }

    public void proxyCheck() {
        info("Verificando se algum Proxy está indisponível...");
        Iterator<Map.Entry<Integer, RMICommon>> iterator = rmiCommons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RMICommon> entry = iterator.next();
            try {
                if (!entry.getValue().isRunning()) {
                    String nome = proxyInfo.stream()
                            .filter(p -> p.getPort() == entry.getKey())
                            .findFirst()
                            .map(ProxyInfo::getName)
                            .orElse("Desconhecido");

                    erro(nome + " não está mais disponível! Removendo da lista...");
                    iterator.remove();
                    proxyInfo.removeIf(p -> p.getPort() == entry.getKey());
                }
            } catch (Exception e) {
                erro("Proxy " + entry.getKey() + " indisponível no momento. Removendo da lista...");
                iterator.remove(); 
                proxyInfo.removeIf(p -> p.getPort() == entry.getKey());
            }
        }

        if (rmiCommons.isEmpty()) {
            erro("Não há nenhum Proxy disponível!");
        }
    }

    @Override
    public void registerProxy(String name, String host, int port, int portRMI) throws Exception {
        ProxyInfo proxyInfo = new ProxyInfo(name, host, port, portRMI);
        this.proxyInfo.addFirst(proxyInfo);
        info("Proxy " + name + " registrado com sucesso!");
        getRmiMethods();
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

    private String formatRMICommons(Map<Integer, RMICommon> rmiCommons) {
        StringBuilder sb = new StringBuilder();
        sb.append("Proxys Ativo(s):\n");

        rmiCommons.forEach((port, instance) -> {
            String className = proxyInfo.stream()
                    .filter(p -> p.getPort() == port)
                    .findFirst()
                    .map(p -> p.getName())
                    .orElse("Desconhecido");
            String runningStatus = "Erro";
            int clients = -1;

            try {
                runningStatus = instance.isRunning() ? "Ativo" : "Inativo";
                clients = instance.numberOfClients();
            } catch (RemoteException e) {
                runningStatus = "Erro de conexão";
            }

            sb.append("  Porta: ").append(port)
                    .append(" -> [").append(className)
                    .append("] Status: ").append(runningStatus)
                    .append(", Clientes: ").append(clients)
                    .append("\n");
        });

        return sb.toString();
    }

    public static void main(String[] args) {
        new LocalizerServer();
    }
}