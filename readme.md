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