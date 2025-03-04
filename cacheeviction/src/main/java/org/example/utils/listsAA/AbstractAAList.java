package org.example.utils.listsAA;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.utils.common.OrderService;
import org.example.utils.exceptions.ElementNotFoundException;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractAAList implements ListAAInterface {
    protected NodeBase head;
    protected NodeBase tail;
    protected int size;

    public void insertFirst(OrderService e) {
        if (head == null) {
            head = new NodeBase(e);
            tail = head;
        } else {
            NodeBase newNode = new NodeBase(e);
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }

        size++;
    }

    public void insertLast(OrderService e) {
        if (tail == null) {
            tail = new NodeBase(e);
            head = tail;
        } else {
            NodeBase newNode = new NodeBase(e);
            newNode.prev = tail;
            tail.next = newNode;
            tail = newNode;
        }

        size++;
    }

    public OrderService peekFirst() throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        return head.data;
    }

    public OrderService peekLast() throws ElementNotFoundException {
        if (tail == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        return tail.data;
    }

    public OrderService peek(int index) throws ElementNotFoundException {
        if (index < 0 || index >= size) {
            throw new ElementNotFoundException("Índice inválido");
        }

        NodeBase current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current.data;
    }

    private OrderService removeNode(NodeBase nodeToRemove) throws ElementNotFoundException {
        if (nodeToRemove == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        OrderService data = nodeToRemove.data;
        if (nodeToRemove == head) {         // Caso especial para a cabeça
            head = head.next;
            if (head != null) {
                head.prev = null;
            }
        } else if (nodeToRemove == tail) {  // Caso especial para a cauda
            tail = tail.prev;
            if (tail != null) {
                tail.next = null;
            }
        }

        size--;
        if (size == 0) {
            head = null;
            tail = null;
        }

        return data;
    }

    public List<OrderService> listAll() throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        List<OrderService> list = new java.util.LinkedList<>();
        NodeBase current = head;
        while (current != null) {
            list.add(current.data);
            current = current.next;
        }

        return list;
    }

    public OrderService removeLast() throws ElementNotFoundException {
        return removeNode(tail);
    }

    public OrderService removeFirst() throws ElementNotFoundException {
        return removeNode(head);
    }

    public OrderService remove(int code) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        NodeBase current = head;

        // Busca o nó com o dado solicitado
        while (current != null) {
            if (current.data.getCode() == code) {
                // Se o nó já está na cabeça, apenas remove
                if (current == head) {
                    return removeFirst();
                }

                // Se o nó já está na cauda, apenas remove
                if (current == tail) {
                    return removeLast();
                }

                // Se o nó está no meio, ajusta os ponteiros para remover o nó da posição atual
                if (current.prev != null) {
                    current.prev.next = current.next;
                }
                if (current.next != null) {
                    current.next.prev = current.prev;
                }

                current.prev = null;
                current.next = null;

                size--;
                if (size == 0) {
                    head = null;
                    tail = null;
                }

                return current.data; // Retorna o valor encontrado
            }
            current = current.next;
        }

        // Se o elemento não foi encontrado, lança uma exceção
        throw new ElementNotFoundException("Elemento não encontrado");
    }

    public void alter(OrderService e) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        NodeBase current = head;

        // Busca o nó com o dado solicitado
        while (current != null) {
            if (current.data.getCode() == e.getCode()) {
                current.data = e; // Altera o valor do nó
                return;
            }
            current = current.next;
        }

        // Se o elemento não foi encontrado, lança uma exceção
        throw new ElementNotFoundException("Elemento não encontrado");
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void show() {
        NodeBase aux = this.head;
        while (aux != null) {
            System.out.println(aux.data);
            aux = aux.next;
        }
    }
}