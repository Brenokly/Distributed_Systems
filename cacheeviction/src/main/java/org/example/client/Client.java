package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import org.example.utils.*;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;

import java.util.List;
import java.util.Scanner;

import static org.example.utils.Command.*;

@Getter
public class Client extends Communicator implements Loggable, JsonSerializable {
    private final int id;                       // ID do cliente
    private final Menu actions;                 // Menu de ações do cliente com base nas opções do servido
    private final Scanner scanner;              // Scanner para leitura de dados
    private boolean closed = false;             // Flag de controle de execução
    private ProxyInfo addressProxy;             // Endereço do servidor proxy
    private static int clients = 0;             // Contador de clientes
    private final ProxyInfo addressLocator;     // Endereço do servidor localizador

    public Client() {
        super("Client " + (++clients));

        this.id = clients;

        this.actions = new Menu();
        this.scanner = new Scanner(System.in);
        this.addressLocator = new ProxyInfo("localhost", 15551);
        // this.addressLocator = new ProxyInfo("26.97.230.179", 15551);

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
                action.run();
            } else {
                System.out.println("Opção inválida!");
            }
        }
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
        connect(addressLocator.getHost(), addressLocator.getPort());

        sendTextMessage("GET_PROXY");

        addressProxy = receiveJsonMessage(ProxyInfo.class);

        if (addressProxy != null) {
            actions.remove(CONECT_LOCATOR);
            actions.put(CONECT_PROXY, this::connectProxy);
        }

        disconnect();
    }

    private void connectProxy() {
        if (addressProxy == null) {
            erro("Não foi possível conectar ao Servidor Proxy. Endereço nulo.");
            return;
        }

        connect(addressProxy.getHost(), addressProxy.getPort());

        if (isConnected()) {
            List<Command> menuOptions = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
            });

            updateMenuAndActions(menuOptions);
        } else {
            erro("Não foi possível conectar ao Servidor Proxy.");
        }
    }

    private void searchOS() {
        sendJsonMessage(SEARCH); // Envia a ação de busca

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o código da OS: ");
        int osCode = scanner.nextInt();
        scanner.nextLine();

        OrderService os = new OrderService();
        os.setCode(osCode);
        sendJsonMessage(os); // Envia a OS com o código para o servidor

        os = receiveJsonMessage(OrderService.class); // Recebe a OS do servidor

        if (os == null) {
            System.out.println("\nOrdem de Serviço não encontrada!");
        } else {
            System.out.println("\nOrdem de Serviço Encontrada: " + os);
        }
    }

    private void registerOS() {
        sendJsonMessage(REGISTER); // Envia a ação de cadastro

        System.out.println("--------------------------------------------------------------");
        System.out.println("Digite os dados da OS");
        OrderService osData = readNewOrder();

        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de cadastro
        System.out.println("\n" + response);
    }

    private void listOS() {
        sendJsonMessage(LIST); // Envia a ação de listagem

        System.out.println("--------------------------------------------------------------");
        List<OrderService> osList = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
        });
        System.out.println("\nLista de OS:");
        if (osList.isEmpty()) {
            System.out.println("Nenhuma OS encontrada!");
        } else {
            osList.forEach(System.out::println);
        }
    }

    private void updateOS() {
        sendJsonMessage(UPDATE); // Envia a ação de alteração

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o ID da OS a ser alterada: ");
        OrderService osData = readOldOrder();

        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de alteração
        System.out.println("\n" + response);
    }

    private void removeOS() {
        sendJsonMessage(REMOVE); // Envia a ação de remoção

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o ID da OS a ser removida: ");
        int osId = scanner.nextInt();
        scanner.nextLine();

        sendTextMessage(String.valueOf(osId));  // Envia o ID para o servidor

        String response = receiveTextMessage();  // Recebe a confirmação de remoção
        System.out.println("\n" + response);
    }

    private void getOSCount() {
        sendJsonMessage(QUANTITY);

        System.out.println("--------------------------------------------------------------");
        int response = Integer.parseInt(receiveTextMessage()); // Recebe a quantidade de registros

        System.out.println("Quantidade de registros: " + response);
    }

    private void authenticate() {
        sendJsonMessage(AUTHENTICATE);

        Command response = INVALID;

        while (response != SUCCESS) {
            System.out.println("--------------------------------------------------------------");
            System.out.print("Digite o login: ");
            String login = scanner.nextLine();
            System.out.print("Digite a senha: ");
            String password = scanner.nextLine();

            sendJsonMessage(new User(login, password));

            response = receiveJsonMessage(Command.class);

            if (response == SUCCESS) {
                info("\nCliente autenticado com sucesso!");
                actions.remove(AUTHENTICATE);
            } else if (ERROR == response) {
                warn("Tu errou as credenciais 3 vezes, serious? Certeza que é Ariel.");
                disconnectClient();
            } else if (INVALID == response) {
                erro("\nCredentials inválidas! Tente novamente.");
            }
        }

        // Recebe as opções do servidor principal
        List<Command> serverCommands = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
        });

        updateMenuAndActions(serverCommands);
    }

    private void disconnectClient() {
        System.out.println("--------------------------------------------------------------");
        sendJsonMessage(DISCONECT);
        info("\nClient " + id + ": Desconectado do Servidor Proxy.");
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
        actions.clearMenu();

        menuOptions.forEach((option) -> actions.put(option, () -> {
            if (option == SEARCH) {
                searchOS();
            } else if (option == REGISTER) {
                registerOS();
            } else if (option == LIST) {
                listOS();
            } else if (option == UPDATE) {
                updateOS();
            } else if (option == REMOVE) {
                removeOS();
            } else if (option == QUANTITY) {
                getOSCount();
            } else if (option == DISCONECT) {
                disconnectClient();
            } else if (option == AUTHENTICATE) {
                authenticate();
            }
        }));
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

    public static void main(String[] args) {
        new Client();
    }
}