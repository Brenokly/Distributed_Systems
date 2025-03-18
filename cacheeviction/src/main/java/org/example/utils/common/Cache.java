package org.example.utils.common;

import lombok.Getter;
import org.example.utils.Loggable;
import org.example.utils.exceptions.ElementNotFoundException;
import org.example.utils.listsAA.LinkedListAAMF;
import java.util.List;

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

public class Cache implements Loggable {
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
        initializerCache();
    }

    private void initializerCache() {
        
    }

    // --------------------------------------------------------------------------------
    // Métodos

    public OrderService search(int code) {
        try {
            OrderService order = cache.search(code);
            hits++;
            info("OrderService encontrado na cache HIT: " + hits + " - " + order);
            return order;
        } catch (ElementNotFoundException e) {
            misses++;
            info("OrderService não encontrado na cache MISS: " + misses);
            return null;
        }
    }

    public void insert(OrderService orderService) {
        if (cache.size() == CAPACIDADE) {
            // Remove o último elemento da cache já que estamos usando a política de cache eviction LRU
            OrderService removed = cache.removeLast();
            info("Cache Cheia, elemento Remvoido: " + removed + " - Tamanho da Cache: " + cache.size());
        }

        cache.insertFirst(orderService);
        info("Elemento inserido na cache: " + orderService + " - Tamanho da Cache: " + cache.size());
    }

    public boolean alter(OrderService orderService) {
        try {
            cache.alter(orderService);
            info("Elemento alterado na cache: " + orderService);
        } catch (ElementNotFoundException e) {
            info("Elemento não encontrado na cache para ser alterado: " + orderService);
            return false;
        }

        return true;
    }

    public boolean remove(int code) {
        try {
            cache.remove(code);
            info("Elemento removido da cache: " + code);
        } catch (ElementNotFoundException e) {
            info("Elemento não encontrado na cache para ser removido: " + code);
            return false;
        }

        return true;
    }

    public List<OrderService> listAll() {
        try {
            return cache.listAll();
        } catch (ElementNotFoundException e) {
            return null;
        }
    }

    public void show() {
        System.out.println("--------------------- Estado da Cache ------------------------");
        cache.show();
        System.out.println("--------------------------------------------------------------");
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }
}