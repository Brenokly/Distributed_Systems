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
        clearLog();
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

    private void providePorts() {
        // Fornece para todos os proxies as portas RMI um aos outros através do método insertPortRmi
        for (ProxyInfo proxy : proxyInfo) {
            try {
                rmiCommons.get(proxy.getPort()).insertPortRmi(proxyInfo.stream()
                        .filter(p -> p.getPort() != proxy.getPort())
                        .map(ProxyInfo::getPortRMI)
                        .toList());
                info("Porta RMI " + proxy.getPortRMI() + " fornecida para o " + proxy.getName() + " com sucesso!");
            } catch (Exception e) {
                erro("Erro ao fornecer a porta RMI " + proxy.getPortRMI() + " para o " + proxy.getName() + ": "
                        + e.getMessage());
            }
        }
    }

    private void createServerSocket() {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST))) {
            this.serverSocket = serverSocket;
            info("Servidor Localizador rodando na porta: " + serverSocket.getLocalPort() + " e no host: "
                    + serverSocket.getInetAddress().getHostAddress());
            running.set(true);
            info("Digite 'stop' para encerrar o Servidor Localizador: ");
    
            while (running.get()) {
                info("Servidor Aguardando conexão de um Cliente...");
                startCommandListener();
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor Localizador!");
            throw new RuntimeException("Erro ao tentar criar o Servidor Localizador!", e);
        }
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
        } finally {
            try {
                communicator.disconnect();
                if (client != null && !client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                logger().error("Erro ao fechar conexão com o cliente!", e);
            }
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
            info("Proxy escolhido: " + chosenProxy.getName() + " com " + (min + 1) + " clientes conectados!");
        } else {
            erro("Nenhum Proxy disponível para balanceamento de carga.");
        }

        return Optional.ofNullable(chosenProxy);
    }

    public void proxyCheck() {
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
                    iterator.remove(); // Remover usando iterator para evitar ConcurrentModificationException
                    proxyInfo.removeIf(p -> p.getPort() == entry.getKey());
                }
            } catch (Exception e) {
                erro("Proxy " + entry.getKey() + " indisponível no momento. Removendo da lista...");
                iterator.remove(); // Remover usando iterator
                proxyInfo.removeIf(p -> p.getPort() == entry.getKey());
            }
        }

        if (rmiCommons.isEmpty()) {
            erro("Nenhum Proxy disponível!");
        } else {
            info(formatRMICommons(rmiCommons));
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
                while (true) {
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
    }

    private String formatRMICommons(Map<Integer, RMICommon> rmiCommons) {
        StringBuilder sb = new StringBuilder();
        sb.append("Proxys Conectados:\n");

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