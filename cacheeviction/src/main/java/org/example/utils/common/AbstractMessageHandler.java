package org.example.utils.common;

import org.example.utils.JsonSerializable;
import org.example.utils.Loggable;
import org.example.utils.common.interfaces.MessageHandler;

import java.io.*;

public class AbstractMessageHandler implements MessageHandler, JsonSerializable, Loggable {
    protected PrintWriter out;
    protected BufferedReader in;

    public AbstractMessageHandler(OutputStream out, InputStream in) {
        this.out = new PrintWriter(out, true);
        this.in = new BufferedReader(new InputStreamReader(in));

        if (this.out == null) {
            throw new RuntimeException("Erro ao tentar abrir Fluxo de Dados!");
        }

        logger().info("Fluxo de Dados aberto com sucesso!");
        System.out.println("Fluxo de Dados aberto com sucesso!");
    }

    public void sendTextMessage(String message) {
        out.println(message);

        logger().info("Mensagem enviada: {}", message);
    }

    public void sendJsonMessage(Object message) {
        if (message != null) {
            String jsonMessage = message instanceof JsonSerializable ? ((JsonSerializable) message).toJson() : message.toString();  // Fallback para string, caso não seja serializável
            sendTextMessage(jsonMessage);
        }

        logger().info("Mensagem enviada: {}", message);
    }

    public String receiveTextMessage() {
        if (in != null) {
            try {
                String message = in.readLine();
                logger().info("Mensagem recebida: {}", message);
                return message;
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
                T objeto = JsonSerializable.objectMapper.readValue(in, clas);

                logger().info("Mensagem recebida: {}", objeto);

                return objeto;
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