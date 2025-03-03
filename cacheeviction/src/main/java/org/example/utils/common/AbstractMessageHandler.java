package org.example.utils.common;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.utils.JsonSerializable;
import org.example.utils.common.interfaces.MessageHandler;

import java.io.*;

public class AbstractMessageHandler implements MessageHandler, JsonSerializable {
    protected PrintWriter out;
    protected BufferedReader in;

    public AbstractMessageHandler(OutputStream out, InputStream in) {
        this.out = new PrintWriter(out, true);
        this.in = new BufferedReader(new InputStreamReader(in));
    }

    public void sendTextMessage(String message) {
        if (out != null && message != null && !message.isEmpty()) {
            out.println(message);
        }
    }

    public void sendJsonMessage(Object message) {
        if (message != null) {
            String jsonMessage = message instanceof JsonSerializable ? ((JsonSerializable) message).toJson() : message.toString();  // Fallback para string, caso não seja serializável
            sendTextMessage(jsonMessage);
        }
    }

    public String receiveTextMessage() {
        if (in != null) {
            try {
                StringBuilder message = new StringBuilder();
                char[] buffer = new char[4096];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    message.append(buffer, 0, bytesRead);
                }

                return message.toString();
            } catch (IOException e) {
                return "Erro: " + e;
            }
        } else {
            return "O buffer de entrada está nulo.";
        }
    }

    public <T> T receiveJsonMessage(Class<T> clas) {
        if (in != null) {
            try {
                return JsonSerializable.objectMapper.readValue(in, clas);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao desserializar JSON", e);
            }
        } else {
            throw new RuntimeException("O buffer de entrada está nulo.");
        }
    }

    public void close() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Erro ao fechar os buffers: " + e.getMessage());
        }
    }
}