import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;

public class ChatServerTCP { // hash é nosso bd
    private static final Map<String, String> usuariosDB = new HashMap<>();
    private static final Map<String, String> nomesDB = new HashMap<>();
    private static final Map<String, String> tokensDB = new HashMap<>();
    private static final List<MensagemDTO> historicoGeral = new ArrayList<>();
    private static final Gson gson = new Gson(); // biblioteca top demais

    static {
        // role-based access control do adm
        usuariosDB.put("admin", "123456");
        nomesDB.put("admin", "Administrador");
        tokensDB.put("admin", "adm");
    }

    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Digite a porta para rodar o Servidor (ex: 8080): ");
        int porta = sc.nextInt();
        // outra barreira de persistencia de erro
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] Aguardando conexoes na porta " + porta + "...\n");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[SERVIDOR] Novo cliente conectado: " + clientSocket.getInetAddress());
                
                // try catch pro servidor persistir a erros
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                    String linha;
                    while ((linha = in.readLine()) != null) {
                        try {
                            // cria a mensagem do servidor com o DTO, tendo a requisição e a resposta
                            System.out.println("-> RECEBIDO: " + linha);
                            MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                            MensagemDTO res = new MensagemDTO();
                            
                            // login realizado com sucesso se e somente se existir a chave no usuario no DB e se o usuario 
                            // e senha condizerem com oq vem do DB, se não da 401 alegando credenciais invalidas, 
                            // tambem ganhamos um token ao logar que é usr_nomeDeUSuario
                            // o sistema tambem fala no chat quem entrou
                            if ("login".equalsIgnoreCase(req.op)) {
                                if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                                    res.resposta = "200"; 
                                    res.token = tokensDB.get(req.usuario); 
                                    res.mensagem = "Login Sucesso";
                                    
                                    String nomeEntrada = nomesDB.get(req.usuario);
                                    historicoGeral.add(new MensagemDTO("Sistema-Enter", null, nomeEntrada + " entrou na sala."));
                                } else { res.resposta = "401"; res.mensagem = "Credenciais invalidas"; }
                            }
                            // le a operação cadastrarUsuario, verifica se a senha é nula ou tem 6 caracteres numéricos, se for da 401
                            // alegando que a senha deve conter exatamente 6 numeros
                            // após isso tem outra verificação para ver se o DB não tem a chave usuário enviada na requisição
                            // e se, não houver ele cadastra o usuario, o nome, a senha, e o token, e retorna 200 alegando usuário cadastrado
                            // se não, ele retorna 401 e alega usuário já existe
                            else if ("cadastrarUsuario".equalsIgnoreCase(req.op)) {
                                if (req.senha == null || !req.senha.matches("\\d{6}")) {
                                    res.resposta = "401"; res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                }
                                else if (!usuariosDB.containsKey(req.usuario)) {
                                    usuariosDB.put(req.usuario, req.senha);
                                    nomesDB.put(req.usuario, req.nome);
                                    tokensDB.put(req.usuario, "usr_" + req.usuario); 
                                    res.resposta = "200"; res.mensagem = "Usuario cadastrado";
                                } else { res.resposta = "401"; res.mensagem = "Usuario ja existe"; }
                            }
                            // Lê a operação atualizarUSuario e verifica se o DB tem a chave usuario da requisição, e busca o token
                            // desse respectivo usuário e vê se é igual ao token vindo da requisição montado concatenando usr_ + nomeDeUsuario
                            // se a condição for 1, então entra no if e cria uma variável que faz um Get e pega a senha do usuário da requisição
                            // para salvar ela para uso na próxima linha, que compara se a nova senha é diferente de nulo, se tem 6 caracteres numéricos 
                            // e se a nova senha é diferente da anterior, caso retorne 1 no primeiro if a senha é trocada, retorna 200 alegando senha alterada com sucesso
                            // e no proximo else if ele verifica se a senha é igual, se for igual ele retorna 401 alegando que a nova senha não pode ser igual a antiga
                            // e no else ele pega a ultima condição que é a senha não ter 6 caracteres numéricos
                            // agora, voltando para o if lá de cima onde faz a verificação do token e se o usuário existe, se isso der falso
                            // retorna 401 alegando Acesso negado, por falta de permissão e token
                            else if ("atualizarUsuario".equalsIgnoreCase(req.op)) {
                                // 1. Identificar o usuário pelo token
                                String usuarioLogado = null;
                                for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                    if (entry.getValue().equals(req.token)) {
                                        usuarioLogado = entry.getKey();
                                        break;
                                    }
                                }

                                if (usuarioLogado != null) {
                                    boolean alterou = false;

                                    // Atualiza o Nome se enviado
                                    if (req.nome != null && !req.nome.trim().isEmpty()) {
                                        nomesDB.put(usuarioLogado, req.nome);
                                        alterou = true;
                                    }

                                    // Atualiza a Senha se enviada (valida 6 dígitos)
                                    if (req.senha != null && req.senha.matches("\\d{6}")) {
                                        String senhaAntiga = usuariosDB.get(usuarioLogado);
                                        if (!req.senha.equals(senhaAntiga)) {
                                            usuariosDB.put(usuarioLogado, req.senha);
                                            alterou = true;
                                        } else {
                                            res.resposta = "401";
                                            res.mensagem = "A nova senha não pode ser igual à antiga.";
                                            out.println(gson.toJson(res));
                                            return; // Encerra para não mandar o 200 lá embaixo
                                        }
                                    }

                                    if (alterou) {
                                        res.resposta = "200";
                                        res.mensagem = "Atualizado com sucesso";
                                    } else {
                                        res.resposta = "401";
                                        res.mensagem = "Nenhum dado válido para atualizar.";
                                    }
                                } else {
                                    res.resposta = "401";
                                    res.mensagem = "Token inválido.";
                                }
                            }
                            //Lê a operação deletarUsuario, primeiro verifica se o usuário a ser deletado
                            // é um adm, se for retorna 401 e alega que o administrador principal nao pode ser apagado, 
                            // depois entra no else if verifica se o usuário que está sendo deletado existe no sistema,
                            // e temos um ou para caso o token do usuário que está deletando é o admin, que tem tal permissão
                            // ficando desta forma a lógica se A^(BvC), A sendo a validação do usuário no sistema, B para ver se o usuário 
                            // logado tem o mesmo token que está sendo apagado, e C para verificar se o token do usuário é o admin, que pode apagar usuários comuns
                            // Lê-se o usuário alvo, remove seu user, seu nome e seu token, retornando 200 e alegando usuário removido
                            // e caso um adm tenha removido a pessoa aparece que o Administrador removeu a pessoa, se a pessoa que apagou a conta
                            // apareceu que apagou a conta e sai, e por fim é adicionado ao historico geral, e caso o primeiro if tenha falhado,
                            // retorna 401 e alega Acesso negado
                            else if ("deletarUsuario".equalsIgnoreCase(req.op)) {
                                // 1. Validar se o token foi enviado
                                if (req.token == null || req.token.trim().isEmpty()) {
                                    res.resposta = "401";
                                    res.mensagem = "Token não pode ser vazio";
                                } else {
                                    // 2. Localizar o login associado ao token
                                    String loginAlvo = null;
                                    for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                        if (entry.getValue().equals(req.token)) {
                                            loginAlvo = entry.getKey();
                                            break;
                                        }
                                    }

                                    if (loginAlvo != null) {
                                        // Trava de segurança para o admin
                                        if ("admin".equalsIgnoreCase(loginAlvo)) {
                                            res.resposta = "401";
                                            res.mensagem = "O Administrador principal nao pode ser apagado.";
                                        } else {
                                            // 3. Executar a remoção de todos os bancos (maps)
                                            String nomeExibicao = nomesDB.get(loginAlvo);
                                            usuariosDB.remove(loginAlvo);
                                            nomesDB.remove(loginAlvo);
                                            tokensDB.remove(loginAlvo);

                                            // 4. Resposta exata conforme o protocolo
                                            res.resposta = "200";
                                            res.mensagem = "Deletado com sucesso";

                                            // Alerta opcional no chat para os outros usuários
                                            historicoGeral.add(new MensagemDTO("Sistema-Delete", null, nomeExibicao + " apagou a conta e saiu."));
                                        }
                                    } else {
                                        res.resposta = "401";
                                        res.mensagem = "Token invalido";
                                    }
                                }
                            }
                            
                            else if ("consultarUsuario".equalsIgnoreCase(req.op)) {
                                // Procuramos qual usuário é o dono do token enviado
                                String loginDono = null;
                                for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                    if (entry.getValue().equals(req.token)) {
                                        loginDono = entry.getKey();
                                        break;
                                    }
                                }

                                if (loginDono != null) {
                                    res.resposta = "200";
                                    res.nome = nomesDB.get(loginDono);
                                    res.usuario = loginDono;
                                    res.token = req.token; 
                                } else {
                                    res.resposta = "401";
                                    res.mensagem = "Token invalido ou expirado.";
                                }
                            }

                            else if ("enviarMensagem".equalsIgnoreCase(req.op)) {
                                String nomeRemetente = nomesDB.get(req.usuario);
                                historicoGeral.add(new MensagemDTO(req.usuario, nomeRemetente, req.texto));
                                res.resposta = "200";
                            }

                            else if ("lerMensagens".equalsIgnoreCase(req.op)) {
                                res.resposta = "200";
                                res.historico = historicoGeral;
                            }
                            else if ("logout".equalsIgnoreCase(req.op)) { 
                                res.resposta = "200";
                                break;
                            }

                            String jsonRes = gson.toJson(res);
                            System.out.println("<- ENVIADO: " + jsonRes + "\n");
                            out.println(jsonRes);

                        } catch (Exception e) { 
                            out.println("{\"resposta\":\"500\", \"mensagem\":\"Erro interno\"}"); 
                        }
                    }
                } catch (IOException e) {
                    // conexão do cliente cai, mas o server n morre
                    System.out.println("[AVISO] Cliente desconectou abruptamente (Connection Reset).");
                } finally {
                    try { clientSocket.close(); } catch (IOException e) {}
                    System.out.println("[SERVIDOR] Conexao encerrada. Aguardando proximo cliente...\n");
                }
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }
}
