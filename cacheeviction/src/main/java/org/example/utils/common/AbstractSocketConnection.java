package org.example.utils.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import lombok.*;
import org.example.utils.Loggable;
import org.example.utils.common.interfaces.SocketConnection;

@Getter
public class AbstractSocketConnection implements SocketConnection, Loggable {
    @Setter
    private int port;
    @Setter
    private String host;
    private Socket socket;
    private InetAddress inetAddress;
    @Getter
    static private int connections = 0;

    public AbstractSocketConnection() {
        connections++;
    }

    public AbstractSocketConnection(Socket socket) {
        this.socket = socket;
        host = socket.getInetAddress().getHostAddress();
        port = socket.getPort();
        inetAddress = socket.getInetAddress();
        connections++;

        System.out.println("Conectado a " + host + ":" + port);
        logger().info("Conectado a {}:{}", host, port);
    }

    @Override
    public synchronized void connect() {
        if (isConnected()) {
            if (inetAddress.getHostName().equals(host) && socket.getPort() == port) {
                System.out.println("Já está conectado a " + inetAddress.getHostName() + ":" + port);
                logger().warn("Já está conectado a {}:{}", inetAddress.getHostName(), port);
                return;
            } else {
                System.out.println("Já está conectado a " + inetAddress.getHostName() + ". Desconectando antes de conectar...");
                logger().warn("Já está conectado a {}. Desconectando antes de conectar...", inetAddress.getHostName());
                disconnect();
            }
        }

        try {
            socket = new Socket(host, port);
            inetAddress = socket.getInetAddress();
            System.out.println("Conectado a " + host + ":" + port);
            logger().info("Conectado a {}:{}", host, port);
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao servidor: " + e.getMessage());
            logger().error("Erro ao conectar ao servidor: {}", e.getMessage());
        }
    }

    @Override
    public void connect(String host, int port) {
        this.host = host;
        this.port = port;
        connect();
    }

    @Override
    public synchronized void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Conexão fechada com o " + inetAddress.getHostName() + ".");
                logger().info("Conexão fechada com o {}.", inetAddress.getHostName());
            }
        } catch (IOException e) {
            System.out.println("Erro ao desconectar: " + e.getMessage());
            logger().error("Erro ao desconectar: {}", e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public Socket getSocket() {
        return socket;
    }
}