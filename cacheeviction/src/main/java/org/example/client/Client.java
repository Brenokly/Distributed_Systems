package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.utils.JsonSerializable;
import org.example.utils.Loggable;
import org.example.utils.Menu;
import org.example.utils.ProxyInfo;
import org.example.utils.common.Communicator;
import org.example.utils.common.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@AllArgsConstructor
@Getter
public class Client extends Communicator implements Loggable, JsonSerializable {
    private static int clients = 0;
    private final int id;
    private final Scanner scanner;
    private ProxyInfo addressProxy;
    private final ProxyInfo addressLocator;
    private final Menu actions = new Menu();

    public Client() {
        this.scanner = new Scanner(System.in);
        this.addressLocator = new ProxyInfo("localhost", 12345);
        this.id = ++clients;

        initializeDefaultActions();
        showMenu();
    }

    private void initializeDefaultActions() {
        actions.put(1, this::connectLocator);
        actions.put(0, this::exitProgram);
    }

    private void showMenu() {
        while (true) {
            System.out.println("\nEscolha uma opção:");

            if (addressProxy == null) {
                System.out.println("1 - Conectar ao Localizador");
            } else if (isClosed()) {
                System.out.println("1 - Conectar ao Servidor Proxy");
            } else if (actions.menuIsValid()) {
                actions.showMenu();
            }
            System.out.println("0 - Sair");

            System.out.print("Opção: ");

            int option = readUserInput();
            Runnable action = actions.get(option);

            if (option == 0) {
                exitProgram();
                return;
            }

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
            return option;
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Opção inválida! Digite um número.");
            return -1;
        }
    }

    private void connectLocator() {
        connect(addressLocator.getHost(), addressLocator.getPort());

        if (isConnected()) {
            sendTextMessage("GET_PROXY");
            addressProxy = receiveJsonMessage(ProxyInfo.class);
            disconnect();
            if (addressProxy != null) {
                actions.put(1, this::connectProxy);
            } else {
                System.out.println("Erro ao obter endereço do servidor Proxy!");
            }
        } else {
            System.out.println("Falha ao conectar ao Localizador!");
        }
    }

    private void connectProxy() {
        if (addressProxy == null) {
            System.out.println("Nenhum Proxy foi encontrado. Conecte-se primeiro ao Localizador!");
            return;
        }

        connect(addressProxy.getHost(), addressProxy.getPort());

        if (isConnected()) {
            sendTextMessage("GET_MENU");
            Map<Integer, String> menuOptions = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
            });
            updateMenuAndActions(menuOptions);
        } else {
            System.out.println("Falha ao conectar ao Servidor Proxy!");
        }
    }

    private void exitProgram() {
        System.out.println("Encerrando Cliente " + id + "...");
        logger().info("Encerrando Cliente {}...", id);
        scanner.close();
        disconnect();
    }

    private void searchOS() {
        System.out.print("Digite o código da OS: ");
        int osCode = scanner.nextInt();
        scanner.nextLine();
        sendJsonMessage(osCode);
        String response = receiveTextMessage();
        System.out.println(response);
    }

    private void registerOS() {
        System.out.println("Digite os dados da OS:");
        OrderService osData = readNewOrder();
        sendJsonMessage(osData);  // Enviar o JSON para o servidor
        String response = receiveTextMessage();
        System.out.println(response);
    }

    private void listOS() {
        sendTextMessage("LIST_OS");
        List<OrderService> osList = JsonSerializable.fromJson(receiveTextMessage(), new TypeReference<>() {
        });
        System.out.println("Lista de OS: " + osList);
    }

    private void updateOS() {
        System.out.print("Digite o ID da OS a ser alterada: ");
        OrderService osData = readOldOrder();
        sendJsonMessage(osData);
        System.out.println("OS alterada com sucesso!");
    }

    private void removeOS() {
        System.out.print("Digite o ID da OS a ser removida: ");
        int osId = scanner.nextInt();
        scanner.nextLine();
        sendJsonMessage(osId);  // Envia o ID para o servidor
        String response = receiveTextMessage();  // Recebe a confirmação de remoção
        System.out.println(response);
    }

    private void getOSCount() {
        sendTextMessage("GET_OS_COUNT");
        // Receber o número de registros de OS
        int response = Integer.parseInt(receiveTextMessage());
        System.out.println("Quantidade de registros: " + response);
    }

    private void updateMenuAndActions(Map<Integer, String> menuOptions) {
        actions.updateMenu(menuOptions);

        Map<Integer, Runnable> menuActions = new HashMap<>();
        menuOptions.forEach((option, description) -> menuActions.put(option, () -> {
            if ("Consultar OS".equals(description)) {
                searchOS();
            } else if ("Cadastrar OS".equals(description)) {
                registerOS();
            } else if ("Listar OS".equals(description)) {
                listOS();
            } else if ("Alterar OS".equals(description)) {
                updateOS();
            } else if ("Remover OS".equals(description)) {
                removeOS();
            } else if ("Quantidade de OS".equals(description)) {
                getOSCount();
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