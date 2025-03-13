package org.example.utils.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMICommon extends Remote {
  boolean isRunning() throws RemoteException;
}