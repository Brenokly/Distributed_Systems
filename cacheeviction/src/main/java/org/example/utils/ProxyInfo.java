package org.example.utils;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProxyInfo implements JsonSerializable {
    public String host;
    public int port;
}