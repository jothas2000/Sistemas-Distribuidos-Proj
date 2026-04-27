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
        usuariosDB.put("admin", "123456");
        tokensDB.put("admin", "adm");
        historicoGeral.add(new MensagemDTO("Sistema-Enter", "Servidor da UTFPR iniciado."));
    }

    public static void main(String args[]) {
        int porta = 8080;
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] Escutando na porta " + porta);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                String linha;
                while ((linha = in.readLine()) != null) {
                    try {
                        MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                        MensagemDTO res = new MensagemDTO();

                        if ("login".equalsIgnoreCase(req.op)) {
                            if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                                res.resposta = "200"; res.token = tokensDB.get(req.usuario); res.mensagem = "Login efetuado com sucesso!";
                                historicoGeral.add(new MensagemDTO("Sistema-Enter", req.usuario + " entrou no chat."));
                            } else {
                                res.resposta = "401"; res.mensagem = "Usuário ou senha inválidos.";
                            }
                        }
                        else if ("create".equalsIgnoreCase(req.op)) {
                            if (usuariosDB.containsKey(req.usuario)) {
                                res.resposta = "401"; res.mensagem = "Erro: Este usuário já existe.";
                            } else if (!req.senha.matches("\\d{6}")) {
                                res.resposta = "401"; res.mensagem = "Erro: A senha deve ter exatamente 6 números.";
                            } else {
                                usuariosDB.put(req.usuario, req.senha);
                                tokensDB.put(req.usuario, "usr_" + req.usuario);
                                res.resposta = "200"; res.mensagem = "Usuário cadastrado com sucesso!";
                            }
                        }
                        else if ("delete".equalsIgnoreCase(req.op)) {
                            if ("adm".equals(req.token)) {
                                if (usuariosDB.containsKey(req.usuario)) {
                                    usuariosDB.remove(req.usuario); tokensDB.remove(req.usuario);
                                    res.resposta = "200"; res.mensagem = "Usuário deletado com sucesso.";
                                    historicoGeral.add(new MensagemDTO("Sistema-Delete", req.usuario + " foi banido pelo Administrador."));
                                } else {
                                    res.resposta = "401"; res.mensagem = "Usuário não encontrado no sistema.";
                                }
                            } else {
                                res.resposta = "401"; res.mensagem = "Permissão negada. Apenas Admin.";
                            }
                        }
                        else if ("send".equalsIgnoreCase(req.op)) {
                            historicoGeral.add(new MensagemDTO(req.usuario, req.texto));
                            res.resposta = "200";
                        }
                        else if ("read".equalsIgnoreCase(req.op)) {
                            res.resposta = "200"; res.historico = historicoGeral;
                        }
                        else if ("bye".equalsIgnoreCase(req.op)) break;

                        out.println(gson.toJson(res));
                    } catch (Exception e) { out.println("{\"resposta\":\"401\"}"); }
                }
                clientSocket.close();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}