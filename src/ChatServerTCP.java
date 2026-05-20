import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;

public class ChatServerTCP { 
    private static final Map<String, String> usuariosDB = new HashMap<>();
    private static final Map<String, String> nomesDB = new HashMap<>();
    private static final Map<String, String> tokensDB = new HashMap<>();
    private static final List<MensagemDTO> historicoGeral = new ArrayList<>();
    private static final Gson gson = new Gson(); 

    static {
        usuariosDB.put("admin", "123456");
        nomesDB.put("admin", "Administrador");
        tokensDB.put("admin", "adm");
    }

    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Digite a porta para rodar o Servidor (ex: 8080): ");
        int porta = sc.nextInt();
        
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] Aguardando conexoes na porta " + porta + "...\n");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                //clientSocket.setSoTimeout(5000); 
                System.out.println("\n[SERVIDOR] Novo cliente conectado: " + clientSocket.getInetAddress());
                
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                    // VARIÁVEL DE SESSÃO: Guarda quem é o dono desta conexão TCP específica
                    String tokenSessaoAtiva = null;

                    String linha;
                    while ((linha = in.readLine()) != null) {
                        try {
                            System.out.println("-> RECEBIDO: " + linha);
                            MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                            MensagemDTO res = new MensagemDTO();
                            
                            // =========================================================================
                            // ======== FILTRO ANTI-SEQUESTRO DE SESSÃO ================================
                            // =========================================================================
                            // Pula a verificação apenas para login e cadastro, pois o usuário ainda não tem token
                            if (!"login".equalsIgnoreCase(req.op) && !"cadastrarUsuario".equalsIgnoreCase(req.op)) {
                                
                                // Verifica qual token o JSON está tentando usar (Admin ou Comum)
                                String tokenEnviado = (req.op != null && req.op.endsWith("Admin")) ? req.token_admin : req.token;
                                
                                // Se o token que chegou no JSON for diferente do token autenticado nesta conexão
                                if (tokenSessaoAtiva == null || !tokenSessaoAtiva.equals(tokenEnviado)) {
                                    res.resposta = "401";
                                    res.mensagem = "Erro de Segurança: O token enviado não pertence à sessão autenticada.";
                                    String jsonRes = gson.toJson(res);
                                    out.println(jsonRes);
                                    System.out.println("[ALERTA] Tentativa de forja de token bloqueada!\n<- ENVIADO: " + jsonRes + "\n");
                                    continue; // Quebra o ciclo aqui e ignora os if/elses abaixo
                                }
                            }
                            // =========================================================================

                            if ("login".equalsIgnoreCase(req.op)) {
                                if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                                    res.resposta = "200"; 
                                    res.token = tokensDB.get(req.usuario); 
                                    res.mensagem = "Login Sucesso";
                                    
                                    // CARIMBA A SESSÃO COM O TOKEN DO USUÁRIO
                                    tokenSessaoAtiva = res.token;
                                    
                                    String nomeEntrada = nomesDB.get(req.usuario);
                                    historicoGeral.add(new MensagemDTO("Sistema-Enter", null, nomeEntrada + " entrou na sala."));
                                } else { res.resposta = "401"; res.mensagem = "Credenciais invalidas"; }
                            }
                            
                            else if ("cadastrarUsuario".equalsIgnoreCase(req.op)) {
                                if (req.senha == null || !req.senha.matches("\\d{6}")) {
                                    res.resposta = "401"; res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                }
                                else if (!usuariosDB.containsKey(req.usuario)) {
                                    usuariosDB.put(req.usuario, req.senha);
                                    nomesDB.put(req.usuario, req.nome);
                                    tokensDB.put(req.usuario, "usr_" + req.usuario); 
                                    res.resposta = "200"; res.mensagem = "Usuario cadastrado";
                                    
                                    // CARIMBA A SESSÃO COM O TOKEN DO NOVO USUÁRIO
                                    tokenSessaoAtiva = "usr_" + req.usuario;
                                } else { res.resposta = "401"; res.mensagem = "Usuario ja existe"; }
                            }
                            
                            else if ("atualizarUsuario".equalsIgnoreCase(req.op)) {
                                String usuarioLogado = null;
                                for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                    if (entry.getValue().equals(req.token)) {
                                        usuarioLogado = entry.getKey();
                                        break;
                                    }
                                }

                                if (usuarioLogado != null) {
                                    boolean alterou = false;

                                    if (req.nome != null && !req.nome.trim().isEmpty()) {
                                        nomesDB.put(usuarioLogado, req.nome);
                                        alterou = true;
                                    }

                                    if (req.senha != null && !req.senha.trim().isEmpty()) {
                                        if (req.senha.matches("\\d{6}")) {
                                            String senhaAntiga = usuariosDB.get(usuarioLogado);
                                            if (!req.senha.equals(senhaAntiga)) {
                                                usuariosDB.put(usuarioLogado, req.senha);
                                                alterou = true;
                                            } else {
                                                res.resposta = "401";
                                                res.mensagem = "A nova senha não pode ser igual à antiga.";
                                                out.println(gson.toJson(res));
                                                continue; 
                                            }
                                        } else {
                                            res.resposta = "401";
                                            res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                            out.println(gson.toJson(res));
                                            continue; 
                                        }
                                    }

                                    if (alterou) {
                                        res.resposta = "200"; res.mensagem = "Atualizado com sucesso";
                                    } else {
                                        res.resposta = "401"; res.mensagem = "Nenhum dado válido para atualizar.";
                                    }
                                } else { res.resposta = "401"; res.mensagem = "Token inválido."; }
                            }
                            
                            else if ("deletarUsuario".equalsIgnoreCase(req.op)) {
                                if (req.token == null || req.token.trim().isEmpty()) {
                                    res.resposta = "401"; res.mensagem = "Token não pode ser vazio";
                                } else {
                                    String loginAlvo = null;
                                    for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                        if (entry.getValue().equals(req.token)) {
                                            loginAlvo = entry.getKey();
                                            break;
                                        }
                                    }

                                    if (loginAlvo != null) {
                                        if ("admin".equalsIgnoreCase(loginAlvo)) {
                                            res.resposta = "401"; res.mensagem = "O Administrador principal nao pode ser apagado.";
                                        } else {
                                            String nomeExibicao = nomesDB.get(loginAlvo);
                                            usuariosDB.remove(loginAlvo);
                                            nomesDB.remove(loginAlvo);
                                            tokensDB.remove(loginAlvo);

                                            res.resposta = "200"; res.mensagem = "Deletado com sucesso";
                                            historicoGeral.add(new MensagemDTO("Sistema-Delete", null, nomeExibicao + " apagou a conta e saiu."));
                                        }
                                    } else { res.resposta = "401"; res.mensagem = "Token invalido"; }
                                }
                            }
                            
                            else if ("consultarUsuario".equalsIgnoreCase(req.op)) {
                                String loginDono = null;
                                for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                    if (entry.getValue().equals(req.token)) {
                                        loginDono = entry.getKey();
                                        break;
                                    }
                                }

                                if (loginDono != null) {
                                    res.resposta = "200"; res.nome = nomesDB.get(loginDono); res.usuario = loginDono; res.token = req.token; 
                                } else { res.resposta = "401"; res.mensagem = "Token invalido ou expirado."; }
                            }

                            else if ("enviarMensagem".equalsIgnoreCase(req.op)) {
                                String nomeRemetente = nomesDB.get(req.usuario);
                                historicoGeral.add(new MensagemDTO(req.usuario, nomeRemetente, req.texto));
                                res.resposta = "200";
                            }

                            else if ("lerMensagens".equalsIgnoreCase(req.op)) {
                                res.resposta = "200"; res.historico = historicoGeral;
                            }

                            else if (req.op != null && req.op.endsWith("Admin")) {
                                boolean isAdmin = false;
                                if (req.token_admin != null && !req.token_admin.trim().isEmpty()) {
                                    String loginAssociado = null;
                                    for (Map.Entry<String, String> entry : tokensDB.entrySet()) {
                                        if (entry.getValue().equals(req.token_admin)) {
                                            loginAssociado = entry.getKey();
                                            break;
                                        }
                                    }
                                    if ("admin".equals(loginAssociado)) {
                                        isAdmin = true;
                                    }
                                }

                                if (!isAdmin) {
                                    res.resposta = "401";
                                    res.mensagem = "Acesso Negado: Credenciais de administrador invalidas.";
                                } else {
                                    if ("consultarUsuariosAdmin".equalsIgnoreCase(req.op)) {
                                        res.resposta = "200";
                                        res.mensagem = "Usuarios listados com sucesso";
                                        res.lista_usuarios = new ArrayList<>();
                                        for (String usrKey : usuariosDB.keySet()) {
                                            res.lista_usuarios.add(new MensagemDTO.UsuarioDTO(nomesDB.get(usrKey), usrKey));
                                        }
                                    } 
                                    else if ("consultarUsuarioAdmin".equalsIgnoreCase(req.op)) {
                                        if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                                            res.resposta = "200";
                                            res.mensagem = "Usuario encontrado";
                                            res.nome = nomesDB.get(req.usuario);
                                            res.usuario = req.usuario;
                                        } else {
                                            res.resposta = "401";
                                            res.mensagem = "Usuario nao encontrado";
                                        }
                                    }
                                    else if ("atualizarUsuarioAdmin".equalsIgnoreCase(req.op)) {
                                        if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                                            boolean alterou = false;
                                            
                                            if (req.nome != null && !req.nome.trim().isEmpty()) {
                                                nomesDB.put(req.usuario, req.nome);
                                                alterou = true;
                                            }
                                            
                                            if (req.senha != null && !req.senha.trim().isEmpty()) {
                                                if (req.senha.matches("\\d{6}")) {
                                                    usuariosDB.put(req.usuario, req.senha);
                                                    alterou = true;
                                                } else {
                                                    res.resposta = "401";
                                                    res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                                    alterou = false; 
                                                }
                                            }
                                            if (alterou) {
                                                res.resposta = "200"; res.mensagem = "Usuario atualizado com sucesso";
                                            } else if (res.resposta == null) {
                                                res.resposta = "401"; res.mensagem = "Nenhum dado valido fornecido.";
                                            }
                                        } else {
                                            res.resposta = "401"; res.mensagem = "Usuario nao encontrado";
                                        }
                                    }
                                    else if ("deletarUsuarioAdmin".equalsIgnoreCase(req.op)) {
                                        if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                                            if ("admin".equalsIgnoreCase(req.usuario)) {
                                                res.resposta = "401";
                                                res.mensagem = "O administrador principal nao pode ser deletado.";
                                            } else {
                                                String nomeExibicao = nomesDB.get(req.usuario);
                                                usuariosDB.remove(req.usuario);
                                                nomesDB.remove(req.usuario);
                                                tokensDB.remove(req.usuario);

                                                res.resposta = "200"; res.mensagem = "Usuario deletado com sucesso";
                                                historicoGeral.add(new MensagemDTO("Sistema-Delete", null, "O Administrador removeu a conta de " + nomeExibicao));
                                            }
                                        } else {
                                            res.resposta = "401"; res.mensagem = "Usuario nao encontrado";
                                        }
                                    }
                                }
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
                } catch (SocketTimeoutException e) {
                    System.out.println("[DEFESA] Cliente derrubado por inatividade (Timeout).");
                } catch (IOException e) {
                    System.out.println("[AVISO] Cliente desconectou abruptamente (Connection Reset).");
                } finally {
                    try { clientSocket.close(); } catch (IOException e) {}
                    System.out.println("[SERVIDOR] Conexao encerrada. Aguardando proximo cliente...\n");
                }
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
            sc.close();
        }
    }
}