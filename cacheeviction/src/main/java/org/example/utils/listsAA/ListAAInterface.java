package org.example.utils.listsAA;

import org.example.utils.common.OrderService;
import org.example.utils.exceptions.ElementNotFoundException;

/*
 * A ListAA é uma interface que define as operações básicas de uma lista autoajustável.
 * Uma lista autoajustável é uma estrutura de dados que reorganiza seus elementos internamente
 * com base em operações de busca, inserção e remoção.
 */

public interface ListAAInterface {
    void insertFirst(OrderService e);

    void insertLast(OrderService e);

    OrderService peekFirst() throws ElementNotFoundException;

    OrderService peekLast() throws ElementNotFoundException;

    OrderService peek(int index) throws ElementNotFoundException;

    OrderService removeFirst() throws ElementNotFoundException;

    OrderService removeLast() throws ElementNotFoundException;

    OrderService remove(int code) throws ElementNotFoundException;

    OrderService search(OrderService e) throws ElementNotFoundException;

    OrderService search(int code) throws ElementNotFoundException;

    void alter(OrderService e) throws ElementNotFoundException;

    int size();

    boolean isEmpty();

    void show();

    String getListState();
}