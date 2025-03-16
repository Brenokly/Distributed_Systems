package org.example.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.example.utils.common.OrderService;

public interface BackupInterface extends Remote {
  void replicateInsert(OrderService order) throws RemoteException;

  void replicateUpdate(OrderService order) throws RemoteException;

  void replicateRemove(int code) throws RemoteException;

  void syncData(List<OrderService> data) throws RemoteException;
}