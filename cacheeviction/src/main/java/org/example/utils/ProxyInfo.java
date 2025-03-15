package org.example.utils;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProxyInfo implements JsonSerializable {
    public String name;
    public String host;
    public int port;
    public int portRMI;
}