# 💬 Chat Distribuído TCP - Entrega 1 (EP-1)

Projeto desenvolvido para a disciplina de Sistemas Distribuídos na UTFPR. Implementa um sistema de Chat Cliente-Servidor utilizando Sockets TCP puros em Java. O sistema foi construído com foco em tolerância a falhas, resiliência de conexões e obediência estrita a um protocolo customizado de troca de mensagens em formato JSON.

## 🏗️ Arquitetura do Sistema

* **Modelo:** Cliente-Servidor Iterativo (Single-threaded). O servidor atende uma requisição por vez, utilizando a fila do SO (*backlog*) para gerenciar as tentativas de conexão.
* **Comunicação:** Síncrona baseada em requisição e resposta.
* **Formatação:** Serialização/Desserialização via JSON com a biblioteca **Google Gson**.

## 📜 Protocolo de Comunicação

O sistema separa operações com responsabilidades únicas, garantindo payloads limpos (sem campos extras indesejados):
* **Sessão:** `login` / `logout` / `cadastrarUsuario`
* **Perfil:** `consultarUsuario` / `atualizarUsuario` / `deletarUsuario` (Identificação estrita via Token).
* **Chat:** `enviarMensagem` / `lerMensagens`

## 🧟‍♂️ O Teste de Resiliência: O "Cliente Zumbi"

Um dos maiores desafios de um servidor iterativo é o travamento por conexões fantasmas (clientes que ocupam o Socket, paralisam a Thread principal e não enviam dados). 

Para demonstrar a resiliência do nosso servidor, o projeto inclui o script de teste acadêmico `ClienteZumbi.java`. Ele simula um ataque de exaustão de conexão enviando um aviso de saída, mas mantendo a conexão TCP "aberta" e silenciosa por 60 segundos.

**Como o servidor se defende:**
Graças ao gerenciamento do ciclo de vida da conexão (Graceful Shutdown via `break` e `finally`), o servidor intercepta a operação de `logout`, toma a iniciativa de quebrar o loop de leitura daquele Socket e encerra a conexão unilateralmente. Isso libera o servidor imediatamente para o próximo usuário da fila, impedindo que o sistema inteiro trave.

## 🚀 Como Compilar e Executar

O projeto utiliza o arquivo `gson-2.10.1.jar` localizado na pasta `lib`. O separador de *classpath* abaixo está configurado para **Windows** (ponto e vírgula `;`). No Linux/Mac, utilize dois pontos (`:`).

**1. Compilar todo o projeto:**
```powershell
javac -d bin -cp ".;lib/gson-2.10.1.jar" src/*.java
2. Executar o Servidor:
# 💬 Chat Distribuído TCP - Entrega 1 (EP-1)

Este projeto implementa um sistema de Chat Cliente-Servidor distribuído utilizando Sockets TCP puros em Java. O sistema foi desenvolvido com foco em tolerância a falhas e segue estritamente um protocolo customizado de troca de mensagens em formato JSON.

## 🏗️ Arquitetura do Sistema

* **Modelo:** Cliente-Servidor Iterativo (Single-threaded). O servidor processa uma requisição por vez, utilizando a fila de espera (*backlog*) do SO para gerenciar múltiplas tentativas de conexão.
* **Comunicação:** Síncrona baseada em requisição e resposta (Pull/Fórum). O cliente envia operações e o servidor responde no formato padrão estipulado pelo protocolo.
* **Formatação:** A serialização e desserialização dos pacotes de rede é feita via JSON utilizando a biblioteca **Google Gson**.

## 📜 Protocolo de Comunicação Implementado

O sistema obedece rigorosamente as restrições do protocolo exigido:

1. **Operações (CRUD completo):**
   * `login` / `logout`
   * `cadastrarUsuario`
   * `consultarUsuario` (Atualizar Histórico / Read)
   * `atualizarUsuario`
   * `deletarUsuario`
   * `enviarMensagem`
2. **Segurança e Validação:**
   * Senhas restritas a **exatamente 6 dígitos numéricos**.
   * Sistema de Autenticação por Tokens (`usr_` para usuários comuns, `adm` para administradores).
3. **Respostas Padronizadas:**
   * Todas as respostas do servidor incluem o campo `"resposta"` com códigos HTTP (ex: `200` Sucesso, `401` Não Autorizado, `500` Erro Interno).
4. **Tolerância a Falhas:**
   * O servidor possui blindagem contra quedas de conexão abruptas (`SocketException: Connection Reset`), garantindo que não trave se um cliente se desconectar inesperadamente.

## 🚀 Como Compilar e Executar

O projeto utiliza o arquivo `gson-2.10.1.jar` localizado na pasta `lib`. O processo de compilação varia levemente dependendo do Sistema Operacional devido ao separador de variáveis de ambiente (`:` no Linux e `;` no Windows).

### No Linux (Terminal)
**1. Compilar os arquivos (enviando para a pasta bin):**
```bash
javac -d bin -cp ".:lib/gson-2.10.1.jar" src/*.java
```
**2. Executar o Servidor:**
```bash
java -cp "bin:lib/gson-2.10.1.jar" ChatServerTCP
```
**3. Executar o Cliente:**
```bash
java -cp "bin:lib/gson-2.10.1.jar" ChatClientGUI
```

### No Windows (PowerShell / CMD)
**1. Compilar os arquivos:**
```powershell
javac -d bin -cp ".;lib/gson-2.10.1.jar" src/*.java
```
**2. Executar o Servidor:**
```powershell
java -cp "bin;lib/gson-2.10.1.jar" ChatServerTCP
```
**3. Executar o Cliente:**
```powershell
java -cp "bin;lib/gson-2.10.1.jar" ChatClientGUI
```

## 🖥️ Utilização
1. Inicie o Servidor e defina a porta de escuta (ex: `8080`).
2. Abra um ou mais Clientes. Informe o IP da máquina onde o servidor está rodando (use `127.0.0.1` se for local ou o IP da rede/Tailscale/Radmin) e a porta.
3. Cadastre um usuário respeitando a regra de senha (6 números).
4. O Histórico Geral suporta identificação visual: mensagens próprias ficam em verde, alertas do sistema em verde/vermelho e as demais em preto.
5. Ainda não suporta multiplos usuários por não ter o sistema de multithreads, o servidor consegue conectar com um usuário de cada vez mantendo em memória as mensagens mandadas por cada um.
PowerShell

java -cp "bin;lib/gson-2.10.1.jar" ChatServerTCP
3. Executar o Cliente (GUI):

PowerShell

java -cp "bin;lib/gson-2.10.1.jar" ChatClientGUI
4. Executar o Teste do Cliente Zumbi (Em um terminal separado):

PowerShell

java -cp "bin;lib/gson-2.10.1.jar" ClienteZumbi


🖥️ Utilização e Interface (GUI)
O cliente (ChatClientGUI) foi desenvolvido em Java Swing e possui:

Gestão de Perfil: Aba exclusiva de Configurações para alteração em tempo real do Nome de Exibição e Senha, validando o Token diretamente com o servidor.

Chat Visual: Identificação por cores. Mensagens do sistema (entradas, saídas e exclusões de contas) são exibidas em verde ou vermelho. Mensagens de usuários são exibidas em preto com os nomes de exibição.

Segurança: Desconexão segura (envio de logout silencioso) automatizada sempre que o cliente volta à tela inicial.


---