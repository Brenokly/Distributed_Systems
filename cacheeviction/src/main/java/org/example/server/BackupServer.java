package org.example.server;

import org.example.utils.Loggable;
import org.example.utils.common.OrderService;
import org.example.utils.exceptions.NodeAlreadyExistsException;
import org.example.utils.tree.TreeAVL;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BackupServer implements BackupInterface, Loggable {
  private int portRMI;
  private String host;
  private TreeAVL treeAVL = new TreeAVL();

  public BackupServer() {
    clearLog("BackupServer");
    portRMI = 13337;
    try {
      this.host = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      erro("Erro ao obter o endereço IP do host: " + e.getMessage());
      this.host = "26.137.178.91";
    }
    configurarRMI();
    createRMI();
  }

  private void configurarRMI() {
    System.setProperty("java.rmi.server.hostname", host);
  }

  private void createRMI() {
    try {
      BackupInterface stub = (BackupInterface) UnicastRemoteObject.exportObject(this, 0);
      Registry registry = LocateRegistry.createRegistry(portRMI);
      registry.bind("backupServer", stub);
      info("Servidor de Backup pronto e rodando na porta " + portRMI);
    } catch (Exception e) {
      erro("Erro ao iniciar o BackupServer: " + e.getMessage());
    }
  }

  @Override
  public void replicateInsert(OrderService order) throws RemoteException {
    try {
      treeAVL.insert(order);
      info("Dado inserido com sucesso:\n" + generateTable(treeAVL.list()));
    } catch (Exception e) {
      erro("Erro ao replicar inserção: " + order);
    }
  }

  @Override
  public void replicateUpdate(OrderService order) throws RemoteException {
    try {
      treeAVL.alter(order);
      info("Dado alterado com sucesso:\n" + generateTable(treeAVL.list()));
    } catch (Exception e) {
      erro("Erro ao replicar atualização: " + order);
    }
  }

  @Override
  public void replicateRemove(int code) throws RemoteException {
    try {
      treeAVL.remove(code);
      info("Dado removido com sucesso:\n" + generateTable(treeAVL.list()));
    } catch (Exception e) {
      erro("Erro ao replicar remoção: " + code);
    }
  }

  @Override
  public void syncData(List<OrderService> data) {
    treeAVL.clear();
    for (OrderService os : data) {
      try {
        treeAVL.insert(os);
      } catch (NodeAlreadyExistsException e) {
        erro("Dado já existe no backup: " + os);
      }
    }
    info("Dados sincronizados com sucesso:\n" + generateTable(treeAVL.list()));
  }

  // Método para exibir a lista em formato de tabela
  private String generateTable(List<OrderService> services) {
    if (services.isEmpty()) {
      return "Nenhum serviço encontrado.";
    }

    StringBuilder table = new StringBuilder();

    // Cabeçalho da tabela
    table.append(String.format("%-6s | %-20s | %-30s | %-8s%n", "Código", "Nome", "Descrição", "Hora"));
    table.append("----------------------------------------------------------------------\n");

    // Adicionando os dados da lista
    for (OrderService service : services) {
      table.append(String.format("%-6d | %-20s | %-30s | %-8s%n",
          service.getCode(),
          service.getName(),
          service.getDescription().length() > 30 ? service.getDescription().substring(0, 27) + "..."
              : service.getDescription(),
          service.getRequestTime()));
    }

    return table.toString();
  }

  public static void main(String[] args) {
    new BackupServer();
  }
}