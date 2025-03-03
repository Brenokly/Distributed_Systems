package org.example.utils.listsAA;

import org.example.utils.common.OrderService;
import org.example.utils.exceptions.ElementNotFoundException;
import org.example.utils.listsAA.NodeBase.NodeFrequency;

/*
 * A LinkedListAA é uma implementação de lista duplamente encadeada com uma
 * funcionalidade adicional de auto-ajuste utilizando o método Count Frequency (CF).
 * O CF é uma técnica onde a ordem dos elementos é ajustada com base na frequência de acessos,
 * promovendo elementos mais acessados para posições de destaque na lista.
 */

public class LinkedListAACF extends AbstractAAList {
    protected NodeFrequency head;
    protected NodeFrequency tail;
    protected int size;

    public LinkedListAACF() {
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

        NodeFrequency current = head;

        while (current != null) {
            if (current.data.getCode() == code) {
                current.frequency++;
                moveUp(current);
                return current.data;
            }
            current = (NodeFrequency) current.next;
        }

        throw new ElementNotFoundException("Elemento não encontrado");
    }

    public void moveUp(NodeFrequency current) {
        if (current == null || current.prev == null) {
            return; // Já está na posição correta
        }

        while (current.prev != null) {
            NodeFrequency prevNode = (NodeFrequency) current.prev;

            if (current.frequency <= prevNode.frequency) {
                break; // O nó já está na posição correta
            }

            // Ajusta os ponteiros para trocar current e prevNode
            NodeFrequency nextNode = (NodeFrequency) current.next;

            if (prevNode.prev != null) {
                prevNode.prev.next = current;
            } else {
                head = current; // Se prevNode era head, agora current se torna a nova head
            }

            if (nextNode != null) {
                nextNode.prev = prevNode;
            } else {
                tail = prevNode; // Se current era tail, prevNode passa a ser a nova tail
            }

            // Realiza a troca dos ponteiros
            current.prev = prevNode.prev;
            prevNode.next = nextNode;
            prevNode.prev = current;
            current.next = prevNode;
        }
    }

    public void show() {
        if (head == null) {
            System.out.println("Lista vazia");
            return;
        }

        NodeFrequency current = head;
        int i = 0; // Índice do elemento na lista

        while (current != null) {
            OrderService os = current.data; // Acessa os dados da ordem de serviço
            System.out.printf("%d: [Código: %d, Nome: %s, Descrição: %s, Hora: %s, Frequência: %d]%n", i, os.getCode(), os.getName(), os.getDescription(), os.getRequestTime(), current.frequency);

            current = (NodeFrequency) current.next; // Atualiza para o próximo nó
            i++;
        }
    }

    public String getListState() {
        StringBuilder sb = new StringBuilder();

        if (isEmpty()) {
            return "Lista vazia";
        }

        NodeFrequency current = head;
        int i = 0; // Índice do elemento na lista

        while (current != null) {
            OrderService os = current.data;
            sb.append(String.format("%d: [Código: %d, Nome: %s, Descrição: %s, Hora: %s, Frequência: %d]%n", i, os.getCode(), os.getName(), os.getDescription(), os.getRequestTime(), current.frequency));

            current = (NodeFrequency) current.next; // Move para o próximo nó
            i++;
        }

        return sb.toString();
    }
}