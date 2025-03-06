# 💾 Projeto de Simulação de Cache Eviction

## Disciplina: Sistemas Distribuídos 📚
### Curso: Ciência da Computação 💻
### Universidade: Ufersa - Universidade Federal Rural do Semi-Árido 🌱
### Ano: 2025 📅

## 📖 Sumário
1. [Introdução](#introducao)
2. [Objetivos](#objetivos)
3. [Conteúdo Programático](#conteudo-programatico)
4. [Prática Offline 2 - Implementação de um Sistema Cliente/Servidor com Cache Eviction](#pratica-offline-2)
   - [Objetivo](#objetivo)
   - [Conceitos-Chave](#conceitos-chave)
   - [Requisitos do Sistema](#requisitos-do-sistema)
   - [Fluxo da Simulação](#fluxo-da-simulacao)
5. [Metodologia](#metodologia)
6. [Estrutura do Projeto](#estrutura-do-projeto)
7. [Referências Bibliográficas](#referencias-bibliograficas)

---

## 📚 Introdução <a id="introducao"></a>
Este documento descreve a implementação de um sistema cliente/servidor com simulação de políticas de Cache Eviction, utilizando conceitos de sistemas distribuídos.

## 🎯 Objetivos <a id="objetivos"></a>
O curso apresenta os principais aspectos, modelos, algoritmos e tecnologias relacionados a sistemas distribuídos. Além disso, fornece fundamentos teóricos e práticos para o desenvolvimento de aplicações distribuídas.

## 📌 Conteúdo Programático <a id="conteudo-programatico"></a>

### Unidade I - Fundamentos de Sistemas Distribuídos (20h)
- Conceitos fundamentais, modelos e arquiteturas.
- Comunicação entre processos.
- Programação distribuída com sockets e RPC.

### Unidade II - Coordenação e Nomeação (20h)
- Nomeação: identificadores e endereços.
- Coordenação: relógios lógicos, exclusão mútua e eleição.

### Unidade III - Replicação e Tolerância a Falhas (20h)
- Modelos de replicação e desafios.
- Abordagens: líder único, múltiplos líderes ou sem líder.

## 🛠️ Prática Offline 2 - Implementação de um Sistema Cliente/Servidor Distribuído <a id="pratica-offline-2"></a>

### 🎯 Objetivo <a id="objetivo"></a>
Desenvolver uma simulação de um sistema cliente/servidor para gerenciar ordens de serviço (OS) com implementação de políticas de Cache Eviction.

### 🏷️ Conceitos-Chave <a id="conceitos-chave"></a>
- **Cache Eviction:** remoção de itens do cache ao atingir a capacidade máxima.
- **Políticas de Remoção:**
  - LRU (Least Recently Used)
  - FIFO (First In, First Out)
  - LFU (Least Frequently Used)
  - Remoção Aleatória
  - MRU (Most Recently Used)
- **Operações:** Hit (cache encontra dado) e Miss (cache não encontra dado).

### 📜 Requisitos do Sistema <a id="requisitos-do-sistema"></a>
- **Cliente:** realiza buscas, cadastro, alteração e remoção de ordens de serviço.
- **Servidor:**
  - Contém servidores de localização, proxy e aplicação.
  - **Servidor Proxy:** gerencia cache (LRU, tamanho 30).
  - **Servidor Principal:** armazenamento em Árvore Balanceada, Tabela Hash ou SGBD.
  - **Servidor de Localização:** fornece IP/PORTA do servidor proxy ao cliente.
  - Registra logs das operações.

### 🔄 Fluxo da Simulação <a id="fluxo-da-simulacao"></a>
1. **Inicialização:**
   - Servidor carrega 100 ordens de serviço.
   - Cache inicia com 27 ordens de serviço.
   - Proxy possui contas pré-carregadas.
2. **Manutenção do Cache:**
   - Exibe estado da cache após cada alteração.
   - FIFO gerencia a política de cache.

## 🏫 Metodologia <a id="metodologia"></a>
- **Técnicas:** Aulas expositivas e práticas.
- **Recursos:** Quadro branco, projetor multimídia, laboratório de computação.
- **Avaliação:** Provas teóricas, práticas e trabalhos individuais e em grupo.

## 📂 Estrutura do Projeto <a id="estrutura-do-projeto"></a>

```
📁 cacheeviction
 ├── 📁 src
 │   ├── 📁 main/java/org/example
 │   │   ├── 📁 client
 │   │   ├── 📁 locator
 │   │   ├── 📁 server
 │   │   ├── 📁 serverproxy
 │   │   ├── 📁 utils
 │   │   │   ├── 📁 common
 │   │   │   ├── 📁 exceptions
 │   │   │   ├── 📁 listsAA
 │   │   │   ├── 📁 tree
 │   │   │   ├── 📄 Command.java
 │   │   │   ├── 📄 JsonSerializable.java
 │   │   │   ├── 📄 Loggable.java
 │   │   │   ├── 📄 Menu.java
 │   │   │   ├── 📄 ProxyInfo.java
 │   │   │   ├── 📄 User.java
```

## 📚 Referências Bibliográficas <a id="referencias-bibliograficas"></a>

### 📖 Obrigatórias:
- Tanenbaum, Andrew S. *Sistemas operacionais modernos*. Pearson Prentice Hall, 2010.
- Coulouris, George. *Sistemas distribuídos: conceitos e projeto*. Bookman, 2013.
- Kurose, James F. *Redes de computadores e a internet: uma abordagem top-down*. Addison Wesley, 2013.

### 📚 Complementares:
- Deitel, Paul J. *Java: como programar*. Pearson Prentice Hall, 2017.
- Chee, Brian J. S. *Computação em nuvem: tecnologias e estratégias*. M. Books do Brasil, 2013.
- Lecheta, Ricardo R. *Web services RESTful*. Novatec, 2015.
- Summerfield, Mark. *Programação em Python 3*. Alta Books, 2012.
---
