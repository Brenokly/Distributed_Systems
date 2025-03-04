package org.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Loggable {
    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    default void info(String message){
        System.out.println(message);
        System.out.println("----------------------------------------------------------");
        logger().info(message);
    }

    default void erro(String message){
        System.out.println(message);
        System.out.println("----------------------------------------------------------");
        logger().error(message);
    }

    default void debug(String message){
        System.out.println(message);
        System.out.println("----------------------------------------------------------");
        logger().debug(message);
    }

    default void warn(String message){
        System.out.println(message);
        System.out.println("----------------------------------------------------------");
        logger().warn(message);
    }
}