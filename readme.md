# 💬 Chat Distribuído TCP - Entrega 2 (EP-2)

Projeto desenvolvido para a disciplina de Sistemas Distribuídos na UTFPR. Implementa um sistema de Chat Cliente-Servidor utilizando Sockets TCP puros em Java. 

A **Entrega 2** expande o projeto inicial com a introdução de um Módulo de Administração, Controle de Acessos Baseado em Funções (RBAC), manipulação de JSON com chaves dinâmicas e defesas avançadas contra ataques de sequestro de sessão.

## 🚀 Novidades da Entrega 2

* **Módulo de Administração:** Rotas exclusivas para o administrador gerenciar o sistema (listar, consultar, atualizar e forçar a exclusão de usuários).
* **Defesa contra Token Forgery (Falsificação de Sessão):** O servidor agora implementa "Sessões Autenticadas". O token é vinculado à conexão TCP no momento do login. Qualquer tentativa de enviar um JSON com um token de terceiros pela mesma conexão é sumariamente bloqueada pelo **Filtro Anti-Sequestro**.
* **Design for Testability (Testabilidade):** A interface gráfica (GUI) foi adaptada com campos de texto editáveis para os tokens. Isso permite que os avaliadores simulem ataques (forjando tokens de Admin ou de outros usuários) para comprovar a eficácia do bloqueio do servidor.
* **Serialização Dinâmica:** Adaptação do `Gson` para suportar as chaves dinâmicas exigidas no protocolo de listagem de usuários (`usuario1`, `nome`, `usuario2`, `nome2`, etc.) através de Listas de Dicionários (`Maps`).
* **Atualizações Parciais Seguras:** Suporte para envio de campos nulos/vazios durante a atualização de perfil, mantendo a validação estrita de 6 dígitos apenas quando a senha é efetivamente alterada.

## 🏗️ Arquitetura do Sistema

* **Modelo:** Cliente-Servidor Iterativo (Single-threaded) com tolerância a falhas e proteção contra exaustão de *Thread/Socket*.
* **Comunicação:** Síncrona baseada em requisição e resposta.
* **Formatação:** Serialização/Desserialização via JSON com a biblioteca **Google Gson**.

## 📜 Protocolo de Comunicação

O sistema obedece estritamente às rotas e restrições definidas no documento base.
* **Sessão:** `login` / `logout` / `cadastrarUsuario`
* **Perfil Comum:** `consultarUsuario` / `atualizarUsuario` / `deletarUsuario`
* **Chat:** `enviarMensagem` / `lerMensagens`
* **Administração (Requer Token 'adm'):** `consultarUsuariosAdmin` / `consultarUsuarioAdmin` / `atualizarUsuarioAdmin` / `deletarUsuarioAdmin`

## 🛡️ Testes de Resiliência (Defesas do Servidor)

1. **Slowloris / Conexões Zumbis:** O servidor utiliza `SoTimeout` de 5 segundos. Clientes inativos ou maliciosos que abrem a porta TCP e não enviam o JSON são desconectados automaticamente.
2. **Graceful Shutdown:** Interceptação correta da rotina de `logout` com quebra de laço (`break`), liberando o servidor iterativo imediatamente para o próximo usuário.
3. **Prevenção de IDOR e Hijacking:** A validação de atualização e exclusão não confia nos dados do cliente. O servidor faz o *reverse lookup* do token no banco de dados em memória para garantir que a sessão ativa tem privilégios sobre o alvo modificado.

## 🚀 Como Compilar e Executar

O projeto utiliza o arquivo `gson-2.10.1.jar` localizado na pasta `lib`. O separador de *classpath* abaixo está configurado para **Windows** (ponto e vírgula `;`). No Linux/Mac, utilize dois pontos (`:`).

**1. Compilar todo o projeto:**
```powershell
javac -d bin -cp ".;lib/gson-2.10.1.jar" src/*.java
2. Executar o Servidor:

PowerShell
java -cp "bin;lib/gson-2.10.1.jar" ChatServerTCP
3. Executar o Cliente (GUI):

PowerShell
java -cp "bin;lib/gson-2.10.1.jar" ChatClientGUI
🔐 Credenciais Padrão do Sistema
O servidor inicializa com os seguintes dados em memória para testes:

Administrador:

Login: admin

Senha: 123456

Token associado: adm

Nota para Avaliação: Para testar a segurança do sistema, faça login como um usuário comum, vá até a aba "Perfil/Config", altere o campo "Modificar Token Usuário (Teste)" para adm e tente atualizar os dados. O servidor bloqueará a ação e emitirá um alerta de tentativa de forja de token no terminal do backend.