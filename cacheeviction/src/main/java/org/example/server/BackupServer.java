package org.example.server;

import org.example.utils.Loggable;
import org.example.utils.common.OrderService;
import org.example.utils.exceptions.NodeAlreadyExistsException;
import org.example.utils.tree.TreeAVL;
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
    clearLog();
    portRMI = 13337;
    host = "26.97.230.179";
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
      info("Registro replicado: " + order);
    } catch (Exception e) {
      erro("Erro ao replicar inserção: " + order);
    }
  }

  @Override
  public void replicateUpdate(OrderService order) throws RemoteException {
    try {
      treeAVL.alter(order);
      info("Registro atualizado: " + order);
    } catch (Exception e) {
      erro("Erro ao replicar atualização: " + order);
    }
  }

  @Override
  public void replicateRemove(int code) throws RemoteException {
    try {
      treeAVL.remove(code);
      info("Registro removido: " + code);
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
    info("Dados sincronizados com sucesso.");
    treeAVL.list().forEach(System.out::println);
  }

  public static void main(String[] args) {
    new BackupServer();
  }
}