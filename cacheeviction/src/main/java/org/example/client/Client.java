package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import org.example.utils.*;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static org.example.utils.Command.*;

@Getter
public class Client extends Communicator implements Loggable, JsonSerializable {
    private final int id;                       // ID do cliente
    private final Menu actions;                 // Menu de ações do cliente com base nas opções do servido
    private final Scanner scanner;              // Scanner para leitura de dados
    private boolean closed = false;             // Flag de controle de execução
    private static int clients = 0;             // Contador de clientes
    private ProxyInfo addressProxy;             // Endereço do servidor proxy
    private final ProxyInfo addressLocator;     // Endereço do servidor localizador

    public Client() {
        super("Client " + (++clients));
        this.id = clients;

        this.actions = new Menu();
        this.scanner = new Scanner(System.in);
        this.addressLocator = new ProxyInfo("Localizador", "10.215.36.169", 14441, 14442);

        initializeDefaultActions();
        showMenu();
    }

    private void initializeDefaultActions() {
        if (addressProxy == null) {
            actions.put(CONECT_LOCATOR, this::connectLocator);
        } else {
            actions.put(CONECT_PROXY, this::connectProxy);
        }
        actions.put(EXIT, this::exitProgram);
    }

    private void showMenu() {
        while (!closed) {
            System.out.println("\nEscolha uma opção:");

            if (actions.menuIsValid()) {
                actions.showMenu();
            }

            System.out.print("Opção: ");

            int option = readUserInput();
            Runnable action = actions.get(option);

            if (action != null) {
                try {
                    action.run();
                } catch (NullPointerException e) {
                    // Lógica para reconectar caso precise
                    erro("Houve um problema com o Proxy ou Servidor. Tentando reconectar...");
                    if (checkOption(option)){
                        disconnect(); // limpa a conexão com o proxy antigo

                        info("Procurando por um novo servidor Proxy...");
                        connectLocator();
                        info("Conectando ao novo servidor Proxy...");
                        connectProxy();
                    }
                }
            } else {
                System.out.println("Opção inválida!");
            }
        }
    }

    private boolean checkOption(int option) {
        return option >= 1 && option <= 6;
    }

    private int readUserInput() {
        try {
            int option = scanner.nextInt();
            scanner.nextLine();
            System.out.println();
            return option;
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Opção inválida! Digite um número.");
            return -1;
        }
    }

    private void connectLocator() {
        try {
            connect(addressLocator.getHost(), addressLocator.getPort()); 

            if (isConnected()) {
                sendTextMessage("GET_PROXY"); // Pede para o locator o IP do proxy!
                addressProxy = receiveJsonMessage(ProxyInfo.class);

                if (addressProxy != null) {
                    actions.remove(CONECT_LOCATOR);
                    actions.put(CONECT_PROXY, this::connectProxy);
                } else {
                    info("Não há servidores Proxy disponíveis no momento. Tente novamente mais tarde.");
                }
            }
        } catch (Exception e) {
            erro("Erro ao tentar conectar ao Localizador: Servidor não encontrado ou indisponível.");
            return;
        }

        disconnect(); // disconecta do locator
    }

    private void connectProxy() {
        if (addressProxy == null) {
            erro("Não foi possível conectar ao Servidor Proxy. Endereço nulo.");
            return;
        }

        connect(addressProxy.getHost(), addressProxy.getPort());

        if (isConnected()) {
            for (int i = 0; i < 3; i++) {
                try {
                    // Recebe as opções do servidor proxy (Autenticar E Desconectar)
                    List<Command> menuOptions = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
                    });
                    updateMenuAndActions(menuOptions);
                    break;
                } catch (Exception e) {
                    erro("Erro ao receber menu ao Proxy: " + e.getMessage());
                }
            } 
        } else {
            erro("Não foi possível conectar ao Servidor Proxy.");
        }
    }

    private void searchOS() throws NullPointerException {
        sendJsonMessage(SEARCH); // Envia a ação de busca

        printSeparator();
        System.out.print("Digite o código da OS: ");
        int osCode = scanner.nextInt();
        scanner.nextLine(); // Limpa o buffer

        OrderService os = new OrderService();
        os.setCode(osCode);
        sendJsonMessage(os); // Envia a OS com o código para o servidor

        os = receiveJsonMessage(OrderService.class); // Recebe a OS do servidor

        if (os.getRequestTime() == null) {
            System.out.println("\nOrdem de Serviço não encontrada!");
        } else {
            System.out.println("\nOrdem de Serviço Encontrada: " + os);
        }
        printSeparator();
    }

    private void registerOS() throws NullPointerException {
        sendJsonMessage(REGISTER); // Envia a ação de cadastro

        printSeparator();
        System.out.println("Digite os dados da OS");
        OrderService osData = readNewOrder();

        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de cadastro
        System.out.println("\n" + response);
        printSeparator();
    }

    private void listOS() throws NullPointerException {
        sendJsonMessage(LIST); // Envia a ação de listagem

        printSeparator();
        List<OrderService> osList = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
        });
        System.out.println("Lista de OS:\n");
        if (osList.isEmpty()) {
            System.out.println("Nenhuma OS encontrada!");
        } else {
            osList.forEach(System.out::println);
        }
        printSeparator();
    }

    private void updateOS() throws NullPointerException {
        sendJsonMessage(UPDATE); // Envia a ação de alteração

        printSeparator();
        System.out.print("Digite o ID da OS a ser alterada: ");
        OrderService osData = readOldOrder();

        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de alteração
        System.out.println("\n" + response);
        printSeparator();
    }

    private void removeOS() throws NullPointerException {
        sendJsonMessage(REMOVE); // Envia a ação de remoção

        printSeparator();
        System.out.print("Digite o ID da OS a ser removida: ");
        int osId = scanner.nextInt();
        scanner.nextLine(); // Limpa o buffer

        sendTextMessage(String.valueOf(osId));  // Envia o ID para o servidor

        String response = receiveTextMessage();  // Recebe a confirmação de remoção
        System.out.println("\n" + response);
        printSeparator();
    }

    private void getOSCount() throws NullPointerException {
        sendJsonMessage(QUANTITY);

        printSeparator();
        int response = Integer.parseInt(receiveTextMessage()); // Recebe a quantidade de registros

        System.out.println("Quantidade de registros: " + response);
        printSeparator();
    }

    private void authenticate() {
        sendJsonMessage(AUTHENTICATE);

        Command response = INVALID;

        while (response != SUCCESS && response != ERROR) {
            sendCredentials();

            response = receiveJsonMessage(Command.class);

            if (response == SUCCESS) {
                info("\nCliente autenticado com sucesso!");
                actions.remove(AUTHENTICATE);
            } else if (response == ERROR) {
                erro("\nVocê errou as credenciais 3 vezes. Certeza que é Ariel, né?");
                info("Client " + id + ": Desconectado do Servidor Proxy tentativas de autenticação excedidas.");

                actions.clearMenu();
                initializeDefaultActions();
                disconnect();

                return;
            } else if (INVALID == response) {
                erro("\nCredentials inválidas! Tente novamente.");
            }
        }

        // Recebe as opções do servidor principal
        List<Command> serverCommands = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {});

        if (serverCommands == null) {
            erro("Erro ao receber menu do servidor principal.");
            disconnect();
            return;
        } 

        updateMenuAndActions(serverCommands);
    }

    private void sendCredentials() {
        printSeparator();
        System.out.print("Digite o login: ");
        String login = scanner.nextLine();

        System.out.print("Digite a senha: ");
        String password = scanner.nextLine();

        sendJsonMessage(new User(login, password));
    }

    private void disconnectClient() {
        printSeparator();

        sendJsonMessage(DISCONECT);

        info("Client " + id + ": Desconectado do Servidor Proxy.");

        disconnect();
        actions.clearMenu();
        initializeDefaultActions();
    }

    private void exitProgram() {
        scanner.close();
        disconnect();
        closed = true;
    }

    private void updateMenuAndActions(List<Command> menuOptions) {
        Map<Command, Runnable> actionsMap = Map.of(
            SEARCH, this::searchOS,
            REGISTER, this::registerOS,
            LIST, this::listOS,
            UPDATE, this::updateOS,
            REMOVE, this::removeOS,
            QUANTITY, this::getOSCount,
            DISCONECT, this::disconnectClient,
            AUTHENTICATE, this::authenticate
        );

        actions.clearMenu();
        menuOptions.forEach(option -> actions.put(option, actionsMap.getOrDefault(option, () -> {
            System.out.println("Ação não reconhecida.");
        })));
    }


    private OrderService readNewOrder() {
        System.out.print("Digite o nome da OS: ");
        String code = scanner.nextLine();

        System.out.print("Digite a descrição da OS: ");
        String description = scanner.nextLine();

        return new OrderService(code, description);
    }

    private OrderService readOldOrder() {
        System.out.print("Digite o código da OS: ");
        int code = scanner.nextInt();

        scanner.nextLine();

        System.out.print("Digite o nome da OS: ");
        String name = scanner.nextLine();

        System.out.print("Digite a descrição da OS: ");
        String description = scanner.nextLine();

        return new OrderService(code, name, description);
    }

    private void printSeparator() {
        System.out.println("--------------------------------------------------------------");
    }

    public static void main(String[] args) {
        new Client();
    }
}