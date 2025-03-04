package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import org.example.utils.*;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.example.utils.Command.*;

@Getter
public class Client extends Communicator implements Loggable, JsonSerializable {
    private static int clients = 0;
    private final int id;
    private final Scanner scanner;
    private ProxyInfo addressProxy;
    private final ProxyInfo addressLocator;
    private final Menu actions;
    private boolean closed = false;

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
        actions.put(CONECT_LOCATOR, this::connectLocator);
        actions.put(EXIT, this::exitProgram);
    }

    private void showMenu() {
        while (!closed) {
            System.out.println("\nEscolha uma opção:");

            if (actions.menuIsValid()) {
                actions.showMenu();
            } else {
                if (addressProxy == null) {
                    System.out.println("7 - Conectar ao Localizador");
                } else if (isClosed()) {
                    System.out.println("8 - Conectar ao Servidor Proxy");
                }
                System.out.println("0 - Sair");
            }

            System.out.print("Opção: ");

            int option = readUserInput();
            Runnable action = actions.get(option);

            if (action != null) {
                action.run();
            } else {
                System.out.println("Opção inválida!");
            }
            System.out.println("--------------------------------------------------------------");
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
            sendTextMessage("GET_MENU");
            Map<Command, String> menuOptions = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
            });
            updateMenuAndActions(menuOptions);
        } else {
            erro("Não foi possível conectar ao Servidor Proxy.");
        }
    }

    private void exitProgram() {
        scanner.close();
        disconnect();
        closed = true;
    }

    private void searchOS() {
        sendJsonMessage(SEARCH); // Envia a ação de busca

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o código da OS: ");
        int osCode = scanner.nextInt();
        scanner.nextLine();

        OrderService os = new OrderService();
        os.setCode(osCode);
        sendJsonMessage(os); // Envia o código da OS para o servidor

        os = receiveJsonMessage(OrderService.class); // Recebe a OS do servidor
        System.out.println(os);
    }

    private void registerOS() {
        sendJsonMessage(REGISTER); // Envia a ação de cadastro

        System.out.println("--------------------------------------------------------------");
        System.out.println("Digite os dados da OS:");
        OrderService osData = readNewOrder();
        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de cadastro
        System.out.println(response);
    }

    private void listOS() {
        sendJsonMessage(LIST); // Envia a ação de listagem

        System.out.println("--------------------------------------------------------------");
        List<OrderService> osList = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
        });
        System.out.println("Lista de OS: " + osList);
    }

    private void updateOS() {
        sendJsonMessage(UPDATE); // Envia a ação de alteração

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o ID da OS a ser alterada: ");
        OrderService osData = readOldOrder();

        sendJsonMessage(osData); // Envia os dados dá OS para o servidor

        String response = receiveTextMessage(); // Recebe a confirmação de alteração
        System.out.println(response);
    }

    private void removeOS() {
        sendJsonMessage(REMOVE); // Envia a ação de remoção

        System.out.println("--------------------------------------------------------------");
        System.out.print("Digite o ID da OS a ser removida: ");
        int osId = scanner.nextInt();
        scanner.nextLine();

        sendJsonMessage(osId);  // Envia o ID para o servidor

        String response = receiveTextMessage();  // Recebe a confirmação de remoção
        System.out.println(response);
    }

    private void getOSCount() {
        sendJsonMessage(QUANTITY);

        System.out.println("--------------------------------------------------------------");
        int response = Integer.parseInt(receiveTextMessage()); // Recebe a quantidade de registros

        System.out.println("Quantidade de registros: " + response);
    }

    private void disconnectClient() {
        System.out.println("--------------------------------------------------------------");
        sendJsonMessage(DISCONECT);
        info("Client" + id + ": Desconectado do Servidor Proxy.");
        actions.clearMenu();
    }

    private void updateMenuAndActions(Map<Command, String> menuOptions) {
        actions.updateMenu(menuOptions);

        Map<Command, Runnable> menuActions = new HashMap<>();
        menuOptions.forEach((option, _) -> menuActions.put(option, () -> {
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
            }
        }));

        actions.updateActions(menuActions);
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
}