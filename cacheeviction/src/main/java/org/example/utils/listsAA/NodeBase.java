package org.example.utils.listsAA;

import org.example.utils.common.OrderService;

public class NodeBase {
    OrderService data;
    NodeBase next;
    NodeBase prev;

    NodeBase(OrderService data) {
        this.data = data;
        this.next = null;
        this.prev = null;
    }

    // Classe derivada com frequência
    public static class Node extends NodeBase {
        Node(OrderService data) {
            super(data);
        }
    }

    // Classe derivada com frequência
    public static class NodeFrequency extends NodeBase {
        int frequency;

        NodeFrequency(OrderService data) {
            super(data);
            this.frequency = 0;
        }
    }
}