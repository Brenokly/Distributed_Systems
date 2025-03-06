# 💾 Projeto de Simulação de Cache Eviction


## Disciplina: Sistemas Distribuídos 📚
### Curso: Ciência da Computação 💻
### Universidade: Ufersa - Universidade Federal Rural do Semi-Árido 🌱
### Ano: 2025 📅

# 2. Objetivos
O curso tem como objetivo apresentar os principais aspectos, características, modelos, algoritmos e tecnologias relacionados a sistemas distribuídos. Também visa mostrar os fundamentos teóricos e práticos para o desenvolvimento e execução de aplicações distribuídas.

# 3. Conteúdo Programático
### Unidade I - Fundamentos de Sistemas Distribuídos (20h)
- Introdução: conceitos fundamentais, objetivos e tipos de sistemas distribuídos.  
- Modelos de sistema e arquiteturas: centralizadas, descentralizadas e híbridas.  
- Processos e threads.  
- Comunicação entre processos: chamada remota de procedimentos (RPC), passagem de mensagem, comunicação em grupo.  
- Programação distribuída com sockets e RPC.

### Unidade II - Coordenação e Nomeação em Sistemas Distribuídos (20h)
- Nomeação: nomes, identificadores e endereços.  
- Coordenação: sincronização de relógios, relógios lógicos.  
- Coordenação: exclusão mútua, algoritmos de eleição e coordenação baseada em gossip.

### Unidade III - Replicação e Tolerância a Falhas (20h)
- Replicação: conceitos e motivação.  
- Modelos de replicação: ativa e passiva.  
- Abordagens: replicar com um único líder, vários líderes ou sem líder.  
- Problemas e desafios da replicação.  
- Tópicos especiais: Tendências em sistemas distribuídos.

# 4. Prática Offline 2 - Implementação de um Sistema Cliente/Servidor com Cache Eviction

## 4.1. Objetivo
Desenvolver uma simulação de um sistema cliente/servidor para gerenciar ordens de serviço (OS) com implementação de políticas de Cache Eviction.

## 4.2. Conceitos-Chave
- Cache Eviction: Processo de remoção de itens do cache quando atinge sua capacidade máxima.
- Principais Políticas:
  - LRU (Least Recently Used)
  - FIFO (First In, First Out)
  - LFU (Least Frequently Used)
  - Remoção Aleatória
  - MRU (Most Recently Used)
- Operações: Hit (cache encontra dado) e Miss (cache não encontra dado).

## 4.3. Requisitos do Sistema
- Cliente:
  - Realiza buscas na base de dados.
  - Cadastra, lista, altera e remove ordens de serviço.
- Servidor:
  - Contém servidor de localização e servidor de aplicação.
  - Gerencia cache (FIFO, tamanho 30).
  - Armazena ordens de serviço em uma Árvore Balanceada, Tabela Hash ou SGBD.
  - Registra logs das operações.

## 4.4. Fluxo da Simulação
1. Inicialização:
   - O servidor carrega 100 ordens de serviço na base de dados.
3. Manutenção do Cache:
   - Exibe estado da cache após cada operação.
   - Implementa FIFO para gerenciamento de cache.

# 5. Metodologia
- Técnicas: Aulas expositivas e práticas em laboratório.
- Recursos: Quadro branco, projetor multimídia, laboratório de computação.
- Avaliação: Provas teóricas, práticas, trabalhos individuais e em grupo.

# 6. Referências Bibliográficas
Obrigatórias:
- Tanenbaum, Andrew S. *Sistemas operacionais modernos*. 3.ed.. Pearson Prentice Hall, 2010.
- Coulouris, George. *Sistemas distribuídos: conceitos e projeto*. 5.ed.. Bookman, 2013.
- Kurose, James F. *Redes de computadores e a internet: uma abordagem top-down*. 6.ed.. Addison Wesley, 2013.

## Complementares:
- Deitel, Paul J. *Java: como programar*. 10.ed.. Pearson Prentice Hall, 2017.
- Chee, Brian J. S. *Computação em nuvem: tecnologias e estratégias*. M. Books do Brasil, 2013.
- Lecheta, Ricardo R. *Web services RESTful*. Novatec, 2015.
- Summerfield, Mark. *Programação em Python 3*. Alta Books, 2012.
