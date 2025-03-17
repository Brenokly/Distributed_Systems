package org.example.serverproxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import org.example.utils.common.OrderService;

public interface ProxyService extends Remote {
    boolean updatePOS(OrderService data) throws RemoteException;
    boolean removePOS(int code) throws RemoteException;
    OrderService searchPOS(int code) throws RemoteException;
    void authenticateClient(String user, String password, String ip) throws RemoteException;
}