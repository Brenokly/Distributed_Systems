README - LinkedListAA (Lista Auto-Ajustável)

Descrição
A LinkedListAA é uma implementação de lista duplamente encadeada com funcionalidades de autoajuste, utilizando dois métodos: 
Move-To-Front (MTF) e Count Frequency (CF). Ambos os métodos visam otimizar o desempenho de busca na lista.

>> Move-To-Front (MTF): Quando um nó é acessado, ele é movido para o início da lista. Essa abordagem otimiza futuras buscas, 
colocando elementos frequentemente utilizados em posições de fácil acesso.

>> Count Frequency (CF): Um método que ajusta a ordem dos elementos com base na frequência de acessos, promovendo elementos mais acessados para 
posições de destaque na lista.

Características

Lista Duplamente Encadeada: Cada nó possui referências para o próximo (next) e o anterior (prev).
Autoajustável com MTF e CF: Suporta os métodos de ajuste MTF e CF para otimizar o desempenho de busca.
Adição e Remoção Dinâmicas: Permite adicionar e remover elementos tanto no início quanto no final da lista.
Manipulação por Código: Cada elemento é identificado pelo seu código através de métodos específicos.

>> Estrutura do Código <<

1. Classe Interna Node

- A classe interna Node representa um nó da lista, contendo:

data: O objeto da classe OrderService.
next: Referência para o próximo nó.
prev: Referência para o nó anterior.

2. Atributos da Classe LinkedListAA

head: O primeiro nó da lista.
tail: O último nó da lista.
size: O tamanho atual da lista.
Métodos da Classe LinkedListAA

- Adição de Elementos
addFirst(OrderService e): Adiciona um novo nó no início da lista.
addLast(OrderService e): Adiciona um novo nó no final da lista.

- Remoção de Elementos
removeFirst(): Remove e retorna o primeiro elemento da lista.
removeLast(): Remove e retorna o último elemento da lista.
remove(int code): Remove um elemento pelo código fornecido.

- Métodos Auxiliares

size(): Retorna o tamanho atual da lista.
isEmpty(): Verifica se a lista está vazia.
peekFirst(): Retorna o primeiro elemento sem removê-lo.
peekLast(): Retorna o último elemento sem removê-lo.
peek(int index): Retorna o elemento em uma posição específica (indexada).

-Considerações Finais

Com essa implementação, a LinkedListAA é autoajustável graças à utilização dos métodos Move-To-Front e Count Frequency. 
Isso significa que elementos frequentemente buscados são movidos para o início da lista ou promovidos com base em sua frequência de acessos, 
reduzindo o tempo de busca em chamadas subsequentes. 
Essa estrutura é especialmente útil em cenários onde alguns elementos são mais acessados do que outros, como em caches ou listas de prioridade.