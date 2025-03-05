package org.example.utils.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.example.utils.JsonSerializable;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderService implements JsonSerializable {
    private static int counterCode = 0;         // Contador de códigos
    private int code;                           // Código do serviço
    private String name;                        // name do serviço
    private String description;                 // Descrição do serviço
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime requestTime;              // Hora da solicitação

    @Builder
    public OrderService(String name, String description) {
        this.code = counterCode++;
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
}