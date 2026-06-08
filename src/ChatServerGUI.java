import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;

public class ChatServerGUI extends JFrame {
    // Bancos de dados em memória (Thread-safe para concorrência do EP-3)
    private static final Map<String, String> usuariosDB = new ConcurrentHashMap<>();
    private static final Map<String, String> nomesDB = new ConcurrentHashMap<>();
    private static final Map<String, String> tokensDB = new ConcurrentHashMap<>();
    
    // Dicionário para rotear mensagens em tempo real (EP-3)
    private static final Map<String, PrintWriter> sessoesAtivas = new ConcurrentHashMap<>();
    
    private static final Gson gson = new Gson();

    // Elementos da Interface Gráfica do Servidor
    private JTextArea areaLogs = new JTextArea();
    private DefaultListModel<String> modeloListaUsuarios = new DefaultListModel<>();
    private JList<String> listaUsuariosUI = new JList<>(modeloListaUsuarios);

    // Semente inicial (Admin)
    static {
        usuariosDB.put("admin", "123456");
        nomesDB.put("admin", "Administrador");
        tokensDB.put("admin", "adm");
    }

    public ChatServerGUI() {
        setTitle("Servidor de Chat - EP3");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        areaLogs.setEditable(false);
        areaLogs.setBackground(Color.BLACK);
        areaLogs.setForeground(Color.GREEN);
        
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setPreferredSize(new Dimension(220, 0));
        painelDireito.add(new JLabel("Usuários Online (Sessões Ativas):", SwingConstants.CENTER), BorderLayout.NORTH);
        painelDireito.add(new JScrollPane(listaUsuariosUI), BorderLayout.CENTER);

        add(new JScrollPane(areaLogs), BorderLayout.CENTER);
        add(painelDireito, BorderLayout.EAST);
    }

    public void registrarLog(String log) {
        SwingUtilities.invokeLater(() -> {
            areaLogs.append(log + "\n");
            areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        });
    }

    public void atualizarListaOnlineUI() {
        SwingUtilities.invokeLater(() -> {
            modeloListaUsuarios.clear();
            for (String user : sessoesAtivas.keySet()) {
                modeloListaUsuarios.addElement(user + " (" + nomesDB.get(user) + ")");
            }
        });
    }

    public void iniciarServidor(int porta) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(porta)) {
                registrarLog("[SERVIDOR] Rodando na porta " + porta);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    registrarLog("[SERVIDOR] Nova conexão: " + clientSocket.getInetAddress());
                    // Inicia uma nova Thread isolada para este cliente (EP-3)
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                registrarLog("[ERRO CRÍTICO] " + e.getMessage());
            }
        }).start();
    }

    // CLASSE INTERNA: Lida com cada cliente em uma Thread separada
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String tokenSessaoAtiva = null;
        private String usuarioSessaoAtiva = null;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                String linha;
                while ((linha = in.readLine()) != null) {
                    registrarLog("-> RECEBIDO: " + linha);
                    MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                    MensagemDTO res = new MensagemDTO();

                    // =========================================================================
                    // 1. FILTRO ANTI-SEQUESTRO DE SESSÃO
                    // =========================================================================
                    if (!"login".equalsIgnoreCase(req.op) && !"cadastrarUsuario".equalsIgnoreCase(req.op)) {
                        String tokenEnviado = (req.op != null && req.op.endsWith("Admin")) ? req.token_admin : req.token;
                        if (tokenSessaoAtiva == null || !tokenSessaoAtiva.equals(tokenEnviado)) {
                            res.resposta = "401"; res.mensagem = "Token inválido ou sessão não autenticada.";
                            out.println(gson.toJson(res));
                            continue;
                        }
                    }

                    // =========================================================================
                    // 2. ROTAS DE SESSÃO E USUÁRIO COMUM (EP-1 e EP-2 restauradas)
                    // =========================================================================
                    if ("login".equalsIgnoreCase(req.op)) {
                        if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                            res.resposta = "200"; 
                            res.token = tokensDB.get(req.usuario); 
                            
                            tokenSessaoAtiva = res.token;
                            usuarioSessaoAtiva = req.usuario;
                            
                            sessoesAtivas.put(usuarioSessaoAtiva, out);
                            atualizarListaOnlineUI();
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
                        if (usuarioSessaoAtiva != null) {
                            boolean alterou = false;

                            if (req.nome != null && !req.nome.trim().isEmpty()) {
                                nomesDB.put(usuarioSessaoAtiva, req.nome);
                                alterou = true;
                            }

                            if (req.senha != null && !req.senha.trim().isEmpty()) {
                                if (req.senha.matches("\\d{6}")) {
                                    String senhaAntiga = usuariosDB.get(usuarioSessaoAtiva);
                                    if (!req.senha.equals(senhaAntiga)) {
                                        usuariosDB.put(usuarioSessaoAtiva, req.senha);
                                        alterou = true;
                                    } else {
                                        res.resposta = "401"; res.mensagem = "A nova senha não pode ser igual à antiga.";
                                        out.println(gson.toJson(res));
                                        continue; 
                                    }
                                } else {
                                    res.resposta = "401"; res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                    out.println(gson.toJson(res));
                                    continue; 
                                }
                            }

                            if (alterou) {
                                res.resposta = "200"; res.mensagem = "Atualizado com sucesso";
                                atualizarListaOnlineUI(); // Caso o nome mude, atualiza a tela
                            } else {
                                res.resposta = "401"; res.mensagem = "Nenhum dado válido para atualizar.";
                            }
                        }
                    }
                    
                    else if ("deletarUsuario".equalsIgnoreCase(req.op)) {
                        if ("admin".equalsIgnoreCase(usuarioSessaoAtiva)) {
                            res.resposta = "401"; res.mensagem = "O Administrador principal nao pode ser apagado.";
                        } else {
                            usuariosDB.remove(usuarioSessaoAtiva);
                            nomesDB.remove(usuarioSessaoAtiva);
                            tokensDB.remove(usuarioSessaoAtiva);
                            sessoesAtivas.remove(usuarioSessaoAtiva);
                            atualizarListaOnlineUI();
                            res.resposta = "200"; res.mensagem = "Deletado com sucesso";
                        }
                    }
                    
                    else if ("consultarUsuario".equalsIgnoreCase(req.op)) {
                        if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                            res.resposta = "200"; 
                            res.usuario = req.usuario; 
                            res.nome = nomesDB.get(req.usuario); 
                        } else { 
                            res.resposta = "401"; res.mensagem = "Usuário alvo não encontrado."; 
                        }
                    }

                    // =========================================================================
                    // 3. ROTAS DE CHAT EM TEMPO REAL E BROADCAST (EP-3)
                    // =========================================================================
                    else if ("ListarUsuariosLogados".equalsIgnoreCase(req.op)) {
                        res.resposta = "200";
                        res.usuarios = new ArrayList<>(sessoesAtivas.keySet());
                    }

                    else if ("enviarMensagem".equalsIgnoreCase(req.op)) {
                        if (sessoesAtivas.containsKey(req.destinatario)) {
                            MensagemDTO pushMsg = new MensagemDTO();
                            pushMsg.op = "receberMensagem";
                            pushMsg.remetente = usuarioSessaoAtiva;
                            pushMsg.mensagem = req.mensagem;
                            
                            PrintWriter outDestino = sessoesAtivas.get(req.destinatario);
                            outDestino.println(gson.toJson(pushMsg));
                            res.resposta = "200"; res.mensagem = "Mensagem enviada";
                        } else {
                            res.resposta = "401"; res.mensagem = "Destinatário offline ou inexistente";
                        }
                    }

                    else if ("enviarBroadcast".equalsIgnoreCase(req.op)) {
                        MensagemDTO pushMsg = new MensagemDTO();
                        pushMsg.op = "receberBroadcast";
                        pushMsg.remetente = usuarioSessaoAtiva;
                        pushMsg.mensagem = req.mensagem;
                        String jsonPush = gson.toJson(pushMsg);
                        
                        for (Map.Entry<String, PrintWriter> entrada : sessoesAtivas.entrySet()) {
                            if (!entrada.getKey().equals(usuarioSessaoAtiva)) {
                                entrada.getValue().println(jsonPush);
                            }
                        }
                        res.resposta = "200"; res.mensagem = "Mensagem enviada a todos";
                    }

                    // =========================================================================
                    // 4. MÓDULO DE ADMINISTRAÇÃO (EP-2 restauradas)
                    // =========================================================================
                    else if (req.op != null && req.op.endsWith("Admin")) {
                        boolean isAdmin = "admin".equals(usuarioSessaoAtiva);

                        if (!isAdmin) {
                            res.resposta = "401"; res.mensagem = "Acesso Negado: Credenciais de administrador invalidas.";
                        } else {
                            if ("consultarUsuariosAdmin".equalsIgnoreCase(req.op)) {
                                res.resposta = "200";
                                res.lista_usuarios = new ArrayList<>();
                                int contador = 1;
                                for (String usrKey : usuariosDB.keySet()) {
                                    Map<String, String> userObj = new LinkedHashMap<>();
                                    userObj.put("usuario" + contador, usrKey);
                                    String chaveNome = (contador == 1) ? "nome" : "nome" + contador;
                                    userObj.put(chaveNome, nomesDB.get(usrKey));
                                    res.lista_usuarios.add(userObj);
                                    contador++;
                                }
                            } 
                            else if ("consultarUsuarioAdmin".equalsIgnoreCase(req.op)) {
                                if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                                    res.resposta = "200"; res.nome = nomesDB.get(req.usuario); res.usuario = req.usuario;
                                } else { res.resposta = "401"; res.mensagem = "Usuario nao encontrado"; }
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
                                            usuariosDB.put(req.usuario, req.senha); alterou = true;
                                        } else {
                                            res.resposta = "401"; res.mensagem = "A senha deve conter exatamente 6 numeros.";
                                            alterou = false; 
                                        }
                                    }
                                    if (alterou) {
                                        res.resposta = "200"; res.mensagem = "Usuario atualizado com sucesso";
                                        atualizarListaOnlineUI();
                                    } else if (res.resposta == null) {
                                        res.resposta = "401"; res.mensagem = "Nenhum dado valido fornecido.";
                                    }
                                } else { res.resposta = "401"; res.mensagem = "Usuario nao encontrado"; }
                            }
                            else if ("deletarUsuarioAdmin".equalsIgnoreCase(req.op)) {
                                if (req.usuario != null && usuariosDB.containsKey(req.usuario)) {
                                    if ("admin".equalsIgnoreCase(req.usuario)) {
                                        res.resposta = "401"; res.mensagem = "O administrador principal nao pode ser deletado.";
                                    } else {
                                        usuariosDB.remove(req.usuario); nomesDB.remove(req.usuario); tokensDB.remove(req.usuario);
                                        sessoesAtivas.remove(req.usuario);
                                        atualizarListaOnlineUI();
                                        res.resposta = "200"; res.mensagem = "Usuario deletado com sucesso";
                                    }
                                } else { res.resposta = "401"; res.mensagem = "Usuario nao encontrado"; }
                            }
                        }
                    }

                    // =========================================================================
                    // 5. SAÍDA
                    // =========================================================================
                    else if ("logout".equalsIgnoreCase(req.op)) { 
                        res.resposta = "200";
                        break; 
                    }

                    // Devolve a resposta
                    if (res.resposta != null) {
                        String jsonRes = gson.toJson(res);
                        registrarLog("<- ENVIADO: " + jsonRes);
                        out.println(jsonRes);
                    }
                }
            } catch (Exception e) {
                registrarLog("[AVISO] Cliente desconectou.");
            } finally {
                if (usuarioSessaoAtiva != null) {
                    sessoesAtivas.remove(usuarioSessaoAtiva);
                    atualizarListaOnlineUI();
                }
                try { socket.close(); } catch (Exception e) {}
            }
        }
    }

    public static void main(String[] args) {
        String portaStr = JOptionPane.showInputDialog("Digite a porta do servidor (ex: 8080):");
        if (portaStr == null || portaStr.trim().isEmpty()) System.exit(0);
        
        ChatServerGUI serverGUI = new ChatServerGUI();
        serverGUI.setVisible(true);
        serverGUI.iniciarServidor(Integer.parseInt(portaStr));
    }
}