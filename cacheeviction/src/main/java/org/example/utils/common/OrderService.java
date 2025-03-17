package org.example.utils.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.utils.JsonSerializable;
import java.io.Serializable;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderService implements JsonSerializable, Serializable {
    private static final long serialVersionUID = 2L;
    private static int counterCode = 0;         // Contador de códigos
    private int code;                           // Código do serviço
    private String name;                        // name do serviço
    private String description;                 // Descrição do serviço
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime requestTime;              // Hora da solicitação

    @Builder
    public OrderService(String name, String description) {
        // No comando de registrar, quem define o código é o servidor
        this.name = name;
        this.description = description;
        this.requestTime = LocalTime.now().withSecond(0).withNano(0);
    }

    @Builder
    public OrderService(int code, String name, String description) {
        if (code > counterCode) {
            counterCode = code;
        }
        this.code = code;
        this.name = name;
        this.description = description;
        this.requestTime = LocalTime.now().withSecond(0).withNano(0);
    }

    @Override
    public String toString() {
        return String.format("[#%d] %s - %s @ %s", 
            code, name, description, requestTime);
    }
}