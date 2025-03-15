package org.example.locator;

import java.rmi.Remote;

public interface LocalizerInterface extends Remote {
  void registerProxy(String name, String host, int port, int portRMI) throws Exception;
}