package org.example.utils.listsAA;

import org.example.utils.common.OrderService;
import org.example.utils.exceptions.ElementNotFoundException;
import org.example.utils.listsAA.NodeBase.Node;

/*
 * A LinkedListAA é uma implementação de lista duplamente encadeada com uma funcionalidade adicional de auto-ajuste utilizando o método Move-To-Front (MTF).
 * O MTF é uma técnica comum em listas auto-ajustáveis, onde um nó que é acessado (buscado) é movido para o início da lista.
 * Essa abordagem otimiza futuras buscas, colocando elementos frequentemente utilizados em posições de fácil acesso.
 */

public class LinkedListAAMF extends AbstractAAList {
    protected Node head;
    protected Node tail;
    protected int size;

    public LinkedListAAMF() {
        head = null;
        tail = null;
        size = 0;
    }

    public OrderService search(OrderService e) throws ElementNotFoundException {
        return search(e.getCode());
    }

    public OrderService search(int code) throws ElementNotFoundException {
        if (head == null) {
            throw new ElementNotFoundException("Lista vazia");
        }

        Node current = head;

        while (current != null) {
            if (current.data.getCode() == code) {
                if (current == head) {
                    return current.data;
                }
                moveToFront(current);
                return current.data;
            }
            current = (Node) current.next;
        }

        throw new ElementNotFoundException("Elemento não encontrado");
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
            tail = (Node) current.prev;
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

            current = (Node) current.next; // Move para o próximo nó
            i++;
        }
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

            current = (Node) current.next; // Move para o próximo nó
            i++;
        }

        return sb.toString();
    }
}