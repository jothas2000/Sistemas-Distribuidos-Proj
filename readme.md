# 💬 Chat Distribuído TCP - UTFPR

Sistema de mensagens multiusuário desenvolvido para a disciplina de **Sistemas Distribuídos** na **UTFPR - Campus Ponta Grossa**. O projeto implementa a comunicação entre processos via **Sockets TCP**, utilizando o formato **JSON** como protocolo de aplicação.

---

## 🏗️ Arquitetura do Sistema

O sistema segue o modelo **Cliente-Servidor Iterativo**. Nesta fase do projeto (**Entrega 1**), o servidor processa **uma requisição por vez**, de forma sequencial.

### 🔧 Componentes

* **ChatServerTCP**
  Responsável por gerenciar o banco de dados em memória (HashMap), autenticar usuários e manter o histórico global de mensagens.

* **ChatClientGUI**
  Interface gráfica desenvolvida em **Swing**, utilizando **CardLayout** para alternância entre telas (Login / Chat).

* **MensagemDTO**
  Objeto de Transferência de Dados responsável por padronizar a comunicação via JSON entre cliente e servidor.

---

## 📋 Protocolo de Aplicação (JSON)

As mensagens trocadas entre cliente e servidor seguem os formatos abaixo:

### 🔐 Autenticação e Usuários

**Login**

```json
{ "op": "login", "usuario": "admin", "senha": "123" }
```

**Cadastro**

```json
{ "op": "create", "usuario": "nome", "senha": "123456" }
```

---

### 💬 Mensageria

**Enviar Mensagem**

```json
{ "op": "send", "usuario": "nome", "texto": "sua mensagem", "token": "seu_token" }
```

**Ler Histórico**

```json
{ "op": "read", "usuario": "nome" }
```

---

## 🛠️ Setup do Projeto

### 📁 Estrutura de Pastas

```
Sistemas-Distribuidos/
├── bin/                 # Binários (.class)
├── lib/                 # Dependências (gson-2.10.1.jar)
├── src/                 # Código-fonte (.java)
└── README.md
```

---

### ⚙️ Compilação e Execução (Linux)

#### 1. Limpar e Compilar

```bash
rm -rf bin/*
javac -d bin -cp ".:lib/gson-2.10.1.jar" src/*.java
```

#### 2. Iniciar o Servidor

```bash
java -cp "bin:lib/gson-2.10.1.jar" ChatServerTCP
```

#### 3. Iniciar o Cliente

```bash
java -cp "bin:lib/gson-2.10.1.jar" ChatClientGUI
```

---

## 🧪 Roteiro de Testes (Importante)

Devido à natureza iterativa do servidor nesta entrega:

* Acesse com o primeiro usuário
* Execute as operações desejadas
* Clique em **Logout**

O botão **Logout** encerra a sessão TCP (`bye`), liberando o servidor para o próximo cliente.

Ao logar com um segundo usuário:

* O histórico de mensagens anterior será carregado automaticamente
* Isso ocorre por meio da operação **read**, exibida na interface de chat

---

## 👨‍🎓 Informações Acadêmicas

* **Acadêmico:** Thales do Prado Menendez
* **Instituição:** Universidade Tecnológica Federal do Paraná (UTFPR - Ponta Grossa)
* **Curso:** Bacharelado em Ciência da Computação

---

## 📌 Observações

Este projeto representa a primeira etapa da implementação de um sistema distribuído, focando em:

* Comunicação via sockets TCP
* Serialização de dados com JSON
* Controle básico de sessões
* Estrutura cliente-servidor sequencial

Futuras melhorias podem incluir:

* Servidor concorrente (multi-thread)
* Persistência em banco de dados
* Melhorias na interface gráfica
* Segurança (criptografia e autenticação robusta)

---
