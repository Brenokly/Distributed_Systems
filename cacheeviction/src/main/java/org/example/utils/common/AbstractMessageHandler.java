package org.example.utils.common;

import org.example.utils.JsonSerializable;
import org.example.utils.Loggable;
import org.example.utils.common.interfaces.MessageHandler;

import java.io.*;

public class AbstractMessageHandler implements MessageHandler, JsonSerializable, Loggable {
    protected PrintWriter out;
    protected BufferedReader in;
    protected String name;

    public AbstractMessageHandler(OutputStream out, InputStream in, String name) {
        this.out = new PrintWriter(out, true);
        this.in = new BufferedReader(new InputStreamReader(in));
        this.name = name;

        if (this.out == null) {
            erro("Erro ao tentar abrir Fluxo de Saída do " + name);
            throw new RuntimeException("Erro ao tentar abrir Fluxo de Saída do " + name);
        }

        info("O " + name + " abriu o Fluxo de Dados com sucesso!");
    }

    public void sendTextMessage(String message) {
        out.println(message);

        message(name + " enviou uma mensagem texto: " + message);
    }

    public void sendJsonMessage(Object message) {
        if (message != null) {
            String jsonMessage = message instanceof JsonSerializable ? ((JsonSerializable) message).toJson() : message.toString();
            out.println(jsonMessage);
            message(name + " enviou uma mensagem json: " + jsonMessage);
        }
    }

    public String receiveTextMessage() {
        if (in != null) {
            try {
                String messagem = in.readLine();
                message(name + " recebeu uma mensagem texto: " + messagem);
                return messagem;
            } catch (IOException e) {
                erro("Erro ao receber mensagem de texto: " + e);
                return "Erro: " + e;
            }
        } else {
            erro("O buffer de entrada está nulo. Fluxo de Dados não aberto.");
            return "O buffer de entrada está nulo. ";
        }
    }

    public <T> T receiveJsonMessage(Class<T> clas) {
        if (in != null) {
            try {

                String json = in.readLine();

                if (json == null) {
                    throw new IOException("Conexão fechada pelo cliente.");
                }

                message(name + " recebeu uma mensagem json: " + json);

                return JsonSerializable.objectMapper.readValue(json, clas);
            } catch (IOException e) {
                erro("Erro ao desserializar JSON: " + e);
                throw new RuntimeException("Erro ao desserializar JSON", e);
            }
        } else {
            erro("O buffer de entrada está nulo. Fluxo de Dados não aberto.");
            throw new RuntimeException("O buffer de entrada está nulo. Fluxo de Dados não aberto.");
        }
    }

    public void close() {
        try {
            if (out != null && in != null) {
                out.close();
                in.close();
                out = null;
                in = null;

                info("O " + name + " fechou o Fluxo de Dados com sucesso!");
            }
        } catch (IOException e) {
            erro("Erro ao fechar Fluxo de Dados: " + e);
            throw new RuntimeException("Erro ao fechar Fluxo de Dados: " + e);
        }
    }
}