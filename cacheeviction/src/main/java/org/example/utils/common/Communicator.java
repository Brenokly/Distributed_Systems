package org.example.utils.common;

import lombok.Getter;

import java.io.IOException;
import java.net.Socket;

@Getter
public class Communicator {
    private final AbstractSocketConnection connection;
    private AbstractMessageHandler ioHandler;

    public Communicator() {
        this.connection = new AbstractSocketConnection();
    }

    public Communicator(Socket socket) {
        this.connection = new AbstractSocketConnection(socket);
        if (connection.isConnected()) {
            createIOHandler();
        }
    }

    public void connect(String host, int port) {
        connection.connect(host, port);
        if (connection.isConnected()) {
            createIOHandler();
        }
    }

    public void createIOHandler() {
        try {
            if (ioHandler != null) {
                ioHandler.close();
            }
            ioHandler = new AbstractMessageHandler(connection.getSocket().getOutputStream(), connection.getSocket().getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao tentar abrir Fluxo de Dados!", e);
        }
    }

    public void disconnect() {
        if (ioHandler != null) ioHandler.close();
        connection.disconnect();
    }

    public void sendTextMessage(String message) {
        ioHandler.sendTextMessage(message);
    }

    public void sendJsonMessage(Object message) {
        ioHandler.sendJsonMessage(message);
    }

    public String receiveTextMessage() {
        return ioHandler.receiveTextMessage();
    }

    public <T> T receiveJsonMessage(Class<T> clas) {
        return ioHandler.receiveJsonMessage(clas);
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean isClosed() {
        return connection.isClosed();
    }

    public Socket getSocket() {
        return connection.getSocket();
    }
}