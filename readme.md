# 💬 Chat Distribuído TCP - Entrega 3 (EP-3)

Projeto desenvolvido para a disciplina de Sistemas Distribuídos na UTFPR. Implementa um sistema de Chat Cliente-Servidor utilizando Sockets TCP em Java. 

A **Entrega 3** marca a evolução arquitetural do sistema de Síncrono (Iterativo) para **Assíncrono (Multi-threaded)**, garantindo uma comunicação simultânea em tempo real (Push Notifications), o que permite o funcionamento perfeito de um chat com múltiplas instâncias concorrentes.

## 🚀 Novidades da Entrega 3 (Tempo Real)

* **Multi-threading no Servidor:** O servidor possui agora a classe `ClientHandler`, instanciando uma Thread dedicada para manter a conexão aberta com cada utilizador, podendo assim sustentar dezenas de conversas em simultâneo.
* **Cliente Assíncrono:** Ao efetuar login, o cliente inicia uma `ReceptorThread` em background que aguarda pelas requisições *push* do servidor (sem bloquear a interface gráfica).
* **Roteamento de Mensagens (Unicast e Broadcast):** Utilizadores podem trocar mensagens privadas ou transmitir uma mensagem de difusão para todos os clientes logados no servidor.
* **Lista de Utilizadores Online (Polling):** A interface do cliente e do servidor implementa um mecanismo que mantém a lista de utilizadores ativos sempre sincronizada, removendo utilizadores que se desconectam abruptamente.
* **GUI do Servidor:** Cumprindo os requisitos de avaliação, o Servidor abandonou o terminal puro e passou a ter a sua própria Janela Gráfica (Java Swing) com registos (logs) e acompanhamento dos clientes em tempo real.

## 🏗️ Arquitetura e Defesas de Segurança (Herança do EP-2)

* **Filtro Anti-Sequestro de Sessão (Token Forgery):** Ao entrar, o socket herda o token do utilizador no lado do servidor. Tentativas de enviar payloads JSON utilizando o token de outro utilizador, a partir de uma conexão já autenticada, são barradas instantaneamente.
* **Módulo Admin e RBAC:** Suporte à hierarquia de acessos, impedindo a exclusão do administrador principal e permitindo gestão plena da base de dados através da GUI do Admin.
* **Design for Testability:** A interface possui campos editáveis de Token (na secção de configurações) para o docente simular e forjar requisições diretas e avaliar a resiliência do servidor perante *Token Hijacking*.

## 📜 Novas Operações do Protocolo (JSON)

* `ListarUsuariosLogados` -> O servidor responde com uma lista (array) de todos os logins ativos.
* `enviarMensagem` / `receberMensagem` -> Roteamento de mensagens privadas (Unicast).
* `enviarBroadcast` / `receberBroadcast` -> Roteamento de mensagens públicas.

## 🚀 Como Compilar e Executar

O projeto utiliza a biblioteca `gson-2.10.1.jar` (localizada na pasta `lib`). O separador de *classpath* abaixo está configurado para **Windows** (ponto e vírgula `;`). No Linux/Mac, utilize dois pontos (`:`).

**1. Compilar todo o projeto:**
javac -d bin -cp ".;lib/gson-2.10.1.jar" src/*.java

**2. Executar o Servidor:**
java -cp "bin;lib/gson-2.10.1.jar" ChatServerTCP

**3. Executar o Cliente:**
java -cp "bin;lib/gson-2.10.1.jar" ChatClientGUI

(Pode abrir múltiplas instâncias do ChatClientGUI para testar a comunicação simultânea entre utilizadores).

🔐 Credenciais Padrão do Sistema
Administrador: Login: admin | Senha: 123456


Todo o ambiente está formatado, assíncrono e apto para testes de invasão e para demonstrar a troca de mensagens com os professores! Qualquer necessidade de afinar ou corrigir interfaces, é só avisar.