package org.example.utils;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyInfo implements JsonSerializable, Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public String host;
    public int port;
    public int portRMI;

    @Override
    public String toString() {
        return String.format("[%s] %s:%d (RMI: %d)", name, host, port, portRMI);
    }
}