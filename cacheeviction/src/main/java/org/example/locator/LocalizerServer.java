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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class LocalizerServer implements Loggable, LocalizerInterface {
    private final int port;                                                         // Porta do Servidor Localizador
    private final int portRMI;                                                      // Porta RMI do Servidor Localizador
    private final String host;                                                      // Host do Servidor Localizador
    private ServerSocket serverSocket;                                              // Socket do Servidor Localizador
    private volatile List<ProxyInfo> proxyInfo;                                     // Informações temporárias dos Proxies
    private volatile Map<Integer, RMICommon> rmiCommons;                            // Proxies ativos e os seus métodos
    private volatile AtomicBoolean running = new AtomicBoolean(false); // Flag para controlar o Servidor Localizador

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
        this.proxyInfo = new ArrayList<>();
        this.rmiCommons = new HashMap<>();
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
        ProxyInfo proxy = this.proxyInfo.getFirst();
        for (int i = 0; i < 3; i++) { // Tenta pegar os métodos do Proxy no RMI 3 vezes para cada Proxy
            try {
                Registry registry = LocateRegistry.getRegistry(host, proxy.getPortRMI());

                rmiCommons.put(proxy.getPort(), (RMICommon) registry.lookup("RMICommon"));
                info("Métodos do " + proxy.getName() + " no RMI pegos com sucesso!");

                providePorts();

                info("Proxies Conectados: " + rmiCommons.toString());
                return;
            } catch (Exception e) {
                erro("Erro ao tentar pegar os métodos do " + proxy.getPort() + " no RMI: Tentativa " + (i + 1) + " de 3");
            }
        }

        erro("Falha ao pegar os métodos do " + proxy.getName() + " após 3 tentativas");
    }
    
    private void providePorts(){
        // Fornece para todos os proxies as portas RMI um aos outros através do método insertPortRmi
        for (ProxyInfo proxy : proxyInfo) {
            try {
                rmiCommons.get(proxy.getPort()).insertPortRmi(proxyInfo.stream()
                        .filter(p -> p.getPort() != proxy.getPort())
                        .map(ProxyInfo::getPortRMI)
                        .toList());
                info("Porta RMI " + proxy.getPortRMI() + " fornecida para o " + proxy.getName() + " com sucesso!");
            } catch (Exception e) {
                erro("Erro ao fornecer a porta RMI " + proxy.getPortRMI() + " para o " + proxy.getName() + ": " + e.getMessage());
            }
        }
    }

    private void createServerSocket() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST));
            info("Servidor Localizador rodando na porta: " + serverSocket.getLocalPort() + " e no host: "
                    + serverSocket.getInetAddress().getHostAddress());
            running.set(true);
            info("Digite 'stop' para encerrar o Servidor Localizador: ");

            while (running.get()) {
                try {
                    info("Servidor Aguardando conexão de um Cliente...");
                    startCommandListener();

                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    erro("Erro ao tentar conectar com o Cliente: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar criar o Servidor Localizador!");
            throw new RuntimeException("Erro ao tentar criar o Servidor Localizador!", e);
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                erro("Erro ao tentar fechar o Servidor Localizador!");
            }
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
                    communicator.sendJsonMessage(loadBalancing());
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
    private ProxyInfo loadBalancing() {
        int min = Integer.MAX_VALUE;
        ProxyInfo proxy = null;

        // Pega o proxy com o menor número de clientes conectados
        for (Map.Entry<Integer, RMICommon> entry : rmiCommons.entrySet()) {
            try {
                int numberOfClients = entry.getValue().numberOfClients();
                if (numberOfClients < min) {
                    min = numberOfClients;
                    proxy = proxyInfo.stream()
                            .filter(p -> p.getPort() == entry.getKey())
                            .findFirst()
                            .orElse(null); // Retorna null ao invés de lançar exceção
                }
            } catch (Exception e) {
                erro("Erro ao tentar pegar o número de clientes conectados no Proxy: " + e.getMessage());
            }
        }

        return proxy;
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
    }

    public static void main(String[] args) {
        new LocalizerServer();
    }
}