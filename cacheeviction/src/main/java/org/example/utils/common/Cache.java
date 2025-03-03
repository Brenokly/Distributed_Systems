package org.example.utils.common;

import lombok.Getter;
import org.example.utils.exceptions.ElementNotFoundException;
import org.example.utils.listsAA.LinkedListAAMF;

/*
 * A classe Cache é responsável por gerenciar a cache de ordens de serviço.
 * Ela foi implementada utilizando uma lista duplamente encadeada autoajustável.
 * A cache possui uma capacidade máxima (30 nesse exemplo), e quando atinge essa capacidade, a política de cache eviction
 * LRU (Least Recently Used) é aplicada, removendo o último elemento da cache.
 *
 * Por que escolhi o LRU?
 *
 * Como se trata de uma lista autoajustável, que move os elementos mais acessados para o início,
 * fica mais fácil de implementar a política de cache eviction LRU, já que o último elemento da lista
 * é, graças a autoajustabilidade, o menos acessado.
 */

public class Cache {
    private final int CAPACIDADE;
    private final LinkedListAAMF cache;
    @Getter
    private int hits = 0;
    @Getter
    private int misses = 0;

    // --------------------------------------------------------------------------------
    // Construtores

    public Cache() {
        this.CAPACIDADE = 30;
        cache = new LinkedListAAMF();
    }

    // --------------------------------------------------------------------------------
    // Métodos

    public OrderService search(int code) {
        try {
            OrderService order = cache.search(code);
            hits++;
            return order;
        } catch (ElementNotFoundException e) {
            misses++;
            return null;
        }
    }

    public void insert(OrderService orderService) {
        if (cache.size() == CAPACIDADE) {
            // Remove o último elemento da cache
            // Já que estamos usando a política de cache eviction LRU
            OrderService removed = cache.removeLast();
        }

        // Insere a nova ordem de serviço
        cache.insertFirst(orderService);
    }

    public boolean alter(OrderService orderService) {
        try {
            cache.alter(orderService);
        } catch (ElementNotFoundException e) {
            return false;
        }

        return true;
    }

    public boolean remove(int code) {
        try {
            cache.remove(code);
        } catch (ElementNotFoundException e) {
            return false;
        }

        return true;
    }

    public void show() {
        cache.show();
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }
}