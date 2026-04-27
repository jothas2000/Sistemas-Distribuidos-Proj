import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;

public class ChatServerTCP {
    private static final Map<String, String> usuariosDB = new HashMap<>();
    private static final Map<String, String> nomesDB = new HashMap<>(); // Guarda o Nome real
    private static final Map<String, String> tokensDB = new HashMap<>();
    private static final List<MensagemDTO> historicoGeral = new ArrayList<>();
    private static final Gson gson = new Gson();

    static {
        // Usuário administrador padrão (RBAC)
        usuariosDB.put("admin", "123456");
        nomesDB.put("admin", "Administrador");
        tokensDB.put("admin", "adm"); // Protocolo exige 'adm'
    }

    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Digite a porta para rodar o Servidor (ex: 8080): ");
        int porta = sc.nextInt();

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] Aguardando conexoes na porta " + porta + "...\n");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[SERVIDOR] Novo cliente conectado: " + clientSocket.getInetAddress());
                
                // NOVO TRY-CATCH: Blinda o servidor contra quedas do cliente
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                    String linha;
                    while ((linha = in.readLine()) != null) {
                        try {
                            System.out.println("-> RECEBIDO: " + linha);
                            MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                            MensagemDTO res = new MensagemDTO();

                            if ("login".equalsIgnoreCase(req.op)) {
                                if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                                    res.resposta = "200"; 
                                    res.token = tokensDB.get(req.usuario); 
                                    res.mensagem = "Login Sucesso";
                                    
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
                                } else { res.resposta = "401"; res.mensagem = "Usuario ja existe"; }
                            }
                            else if ("atualizarUsuario".equalsIgnoreCase(req.op)) { 
                                if (usuariosDB.containsKey(req.usuario) && tokensDB.get(req.usuario).equals(req.token)) {
                                    if (req.novaSenha != null && req.novaSenha.matches("\\d{6}")) {
                                        usuariosDB.put(req.usuario, req.novaSenha);
                                        res.resposta = "200"; res.mensagem = "Senha alterada com sucesso";
                                    } else { res.resposta = "401"; res.mensagem = "Nova senha deve ter 6 numeros."; }
                                } else { res.resposta = "401"; res.mensagem = "Acesso negado"; }
                            }
                            else if ("deletarUsuario".equalsIgnoreCase(req.op)) { 
                                if ((usuariosDB.containsKey(req.usuario) && tokensDB.get(req.usuario).equals(req.token)) || "adm".equals(req.token)) {
                                    String nomeAlvo = nomesDB.get(req.usuario);
                                    usuariosDB.remove(req.usuario); 
                                    tokensDB.remove(req.usuario);
                                    nomesDB.remove(req.usuario);
                                    res.resposta = "200"; res.mensagem = "Usuario removido";
                                    
                                    String textoAlerta = "adm".equals(req.token) ? 
                                        "Administrador removeu " + nomeAlvo + " da sala." : 
                                        nomeAlvo + " apagou a conta e saiu.";
                                    
                                    historicoGeral.add(new MensagemDTO("Sistema-Delete", null, textoAlerta));
                                } else { res.resposta = "401"; res.mensagem = "Acesso negado"; }
                            }
                            else if ("consultarUsuario".equalsIgnoreCase(req.op)) {
                                res.resposta = "200"; res.historico = historicoGeral;
                            }
                            else if ("enviarMensagem".equalsIgnoreCase(req.op)) {
                                String nomeRemetente = nomesDB.get(req.usuario);
                                historicoGeral.add(new MensagemDTO(req.usuario, nomeRemetente, req.texto));
                                res.resposta = "200";
                            }
                            else if ("logout".equalsIgnoreCase(req.op)) { 
                                res.resposta = "200";
                                // Opcional: out.println(gson.toJson(res)); 
                                break; // Sai do laço de leitura e fecha conexão
                            }

                            String jsonRes = gson.toJson(res);
                            System.out.println("<- ENVIADO: " + jsonRes + "\n");
                            out.println(jsonRes);

                        } catch (Exception e) { 
                            out.println("{\"resposta\":\"500\", \"mensagem\":\"Erro interno\"}"); 
                        }
                    }
                } catch (IOException e) {
                    // SE O CLIENTE DESLIGAR NA CARA, CAI AQUI, MAS O SERVIDOR NÃO MORRE!
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