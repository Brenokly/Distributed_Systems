package org.example.utils.listsAA;

import org.example.utils.common.OrderService;
import org.example.utils.exceptions.ElementNotFoundException;

import java.util.List;

/*
 * A LinkedListAA é uma implementação de lista duplamente encadeada com uma funcionalidade adicional de auto-ajuste utilizando o método Move-To-Front (MTF).
 * O MTF é uma técnica comum em listas auto-ajustáveis, onde um nó que é acessado (buscado) é movido para o início da lista.
 * Essa abordagem otimiza futuras buscas, colocando elementos frequentemente utilizados em posições de fácil acesso.
 */

public class LinkedListAAMF {
    static class Node {
        OrderService data;
        Node next;
        Node prev;

        Node(OrderService data) {
            this.data = data;
            this.next = null;
            this.prev = null;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public LinkedListAAMF() {
        head = null;
        tail = null;
        size = 0;
    }

    public void insertFirst(OrderService e) {
        if (head == null) {
            head = new Node(e);
            tail = head;
        } else {
            Node newNode = new Node(e);
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }

        size++;
    }

    public void insertLast(OrderService e) {
        if (tail == null) {
            tail = new Node(e);
            head = tail;
        } else {
            Node newNode = new Node(e);
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

        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current.data;
    }

    public OrderService removeFirst() throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        OrderService data = head.data;
        head = head.next;
        size--;

        if (size == 0) {
            tail = null;
        } else {
            head.prev = null;
        }

        return data;
    }

    public OrderService removeLast() throws ElementNotFoundException {
        if (tail == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        OrderService data = tail.data;
        tail = tail.prev;
        size--;

        if (size == 0) {
            head = null;
        } else {
            tail.next = null;
        }

        return data;
    }

    public OrderService remove(int code) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        Node current = head;

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

                return current.data; // Retorna o valor encontrado
            }
            current = current.next;
        }

        // Se o elemento não foi encontrado, lança uma exceção
        throw new ElementNotFoundException("Lista vazia");
    }

    public OrderService search(OrderService e) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        Node current = head;

        // Busca o nó com o dado solicitado
        while (current != null) {
            if (current.data.getCode() == e.getCode()) {
                // Se o nó já está na cabeça, apenas retorna
                if (current == head) {
                    return current.data;
                }

                // Move o nó para o início
                moveToFront(current);

                return current.data; // Retorna o valor encontrado
            }
            current = current.next;
        }

        // Se o elemento não foi encontrado, lança uma exceção
        throw new ElementNotFoundException("Elemento não encontrado");
    }

    public OrderService search(int code) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        Node current = head;

        // Busca o nó com o dado solicitado
        while (current != null) {
            if (current.data.getCode() == code) {
                // Se o nó já está na cabeça, apenas retorna
                if (current == head) {
                    return current.data;
                }

                // Move o nó para o início
                moveToFront(current);

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

        Node current = head;

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

    public List<OrderService> listAll() throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        List<OrderService> list = new java.util.ArrayList<>();
        Node current = head;

        while (current != null) {
            list.add(current.data);
            current = current.next;
        }

        return list;
    }

    public void moveToFront(Node current) {
        if (current == head) {
            return;
        }

        // Ajusta os ponteiros para remover o nó da posição atual
        if (current.prev != null) {
            current.prev.next = current.next;
        }
        if (current.next != null) {
            current.next.prev = current.prev;
        }

        // Atualiza o tail se o nó era o último
        if (current == tail) {
            tail = current.prev;
        }

        // Move o nó para o início
        current.next = head;
        head.prev = current;
        current.prev = null;
        head = current;
    }

    public void show() {
        if (head == null) {
            System.out.println("Lista vazia");
            return;
        }

        Node current = head;
        int i = 0; // Índice do elemento na lista

        while (current != null) {
            OrderService os = current.data; // Acessa os dados da ordem de serviço
            System.out.printf("%d: [Código: %d, Nome: %s, Descrição: %s, Hora: %s]%n", i, os.getCode(), os.getName(), os.getDescription(), os.getRequestTime());

            current = current.next; // Move para o próximo nó
            i++;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public String getListState() {
        StringBuilder sb = new StringBuilder();

        if (isEmpty()) {
            return "Lista vazia";
        }

        Node current = head;
        int i = 0; // Índice do elemento na lista

        while (current != null) {
            OrderService os = current.data;
            sb.append(String.format("%d: [Código: %d, Nome: %s, Descrição: %s, Hora: %s]%n", i, os.getCode(), os.getName(), os.getDescription(), os.getRequestTime()));

            current = current.next; // Move para o próximo nó
            i++;
        }

        return sb.toString();
    }
}