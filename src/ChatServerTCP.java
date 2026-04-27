import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;

public class ChatServerTCP {
    private static final Map<String, String> usuariosDB = new HashMap<>();
    private static final Map<String, String> tokensDB = new HashMap<>();
    private static final List<MensagemDTO> historicoGeral = new ArrayList<>();
    private static final Gson gson = new Gson();

    static {
        // Usuário administrador padrão (RBAC)
        usuariosDB.put("admin", "123456");
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
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                String linha;
                while ((linha = in.readLine()) != null) {
                    try {
                        System.out.println("-> RECEBIDO: " + linha);
                        MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                        MensagemDTO res = new MensagemDTO();

                        // Lógica do CRUD e Mensageria
                        if ("login".equalsIgnoreCase(req.op)) {
                            if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                                res.resposta = "200"; res.token = tokensDB.get(req.usuario); res.mensagem = "Login Sucesso";
                            } else { res.resposta = "401"; res.mensagem = "Credenciais invalidas"; }
                        }
                        else if ("create".equalsIgnoreCase(req.op)) {
                            if (!usuariosDB.containsKey(req.usuario)) {
                                usuariosDB.put(req.usuario, req.senha);
                                tokensDB.put(req.usuario, "token_" + req.usuario);
                                res.resposta = "200"; res.mensagem = "Usuario cadastrado com sucesso";
                            } else { res.resposta = "401"; res.mensagem = "Usuario ja existe"; }
                        }
                        else if ("update".equalsIgnoreCase(req.op)) { 
                            if (usuariosDB.containsKey(req.usuario) && tokensDB.get(req.usuario).equals(req.token)) {
                                usuariosDB.put(req.usuario, req.novaSenha);
                                res.resposta = "200"; res.mensagem = "Senha alterada com sucesso";
                            } else { res.resposta = "401"; res.mensagem = "Acesso negado"; }
                        }
                        else if ("delete".equalsIgnoreCase(req.op)) { 
                            // Permite apagar a própria conta ou apagar qualquer uma se for admin
                            if ((usuariosDB.containsKey(req.usuario) && tokensDB.get(req.usuario).equals(req.token)) || "adm".equals(req.token)) {
                                usuariosDB.remove(req.usuario); 
                                tokensDB.remove(req.usuario);
                                res.resposta = "200"; res.mensagem = "Usuario removido";
                            } else { res.resposta = "401"; res.mensagem = "Acesso negado"; }
                        }
                        else if ("read".equalsIgnoreCase(req.op)) {
                            res.resposta = "200"; res.historico = historicoGeral;
                        }
                        else if ("send".equalsIgnoreCase(req.op)) {
                            historicoGeral.add(new MensagemDTO(req.usuario, req.texto));
                            res.resposta = "200";
                        }
                        else if ("bye".equalsIgnoreCase(req.op)) {
                            break; 
                        }

                        String jsonRes = gson.toJson(res);
                        System.out.println("<- ENVIADO: " + jsonRes + "\n");
                        out.println(jsonRes);
                    } catch (Exception e) { 
                        out.println("{\"resposta\":\"500\", \"mensagem\":\"Erro interno no servidor\"}"); 
                    }
                }
                clientSocket.close();
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }
}