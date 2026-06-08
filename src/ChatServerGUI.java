import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;

public class ChatServerGUI extends JFrame {
    // Bancos de dados em memória
    private static final Map<String, String> usuariosDB = new ConcurrentHashMap<>();
    private static final Map<String, String> nomesDB = new ConcurrentHashMap<>();
    private static final Map<String, String> tokensDB = new ConcurrentHashMap<>();
    
    // EP-3: Dicionário para rotear mensagens. Mapeia o Login -> Canal de Saída (PrintWriter)
    private static final Map<String, PrintWriter> sessoesAtivas = new ConcurrentHashMap<>();
    
    private static final Gson gson = new Gson();

    // Elementos da Interface Gráfica do Servidor
    private JTextArea areaLogs = new JTextArea();
    private DefaultListModel<String> modeloListaUsuarios = new DefaultListModel<>();
    private JList<String> listaUsuariosUI = new JList<>(modeloListaUsuarios);

    static {
        usuariosDB.put("admin", "123456");
        nomesDB.put("admin", "Administrador");
        tokensDB.put("admin", "adm");
    }

    public ChatServerGUI() {
        setTitle("Servidor de Chat - EP3");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        areaLogs.setEditable(false);
        areaLogs.setBackground(Color.BLACK);
        areaLogs.setForeground(Color.GREEN);
        
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setPreferredSize(new Dimension(200, 0));
        painelDireito.add(new JLabel("Usuários Online:", SwingConstants.CENTER), BorderLayout.NORTH);
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
                    // Inicia uma nova Thread para este cliente
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
                // Removemos o Timeout para o EP-3, pois a conexão precisa ficar aberta esperando mensagens
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                String linha;
                while ((linha = in.readLine()) != null) {
                    registrarLog("-> RECEBIDO: " + linha);
                    MensagemDTO req = gson.fromJson(linha, MensagemDTO.class);
                    MensagemDTO res = new MensagemDTO();

                    // Filtro Anti-Sequestro de Sessão (Mantido do EP-2)
                    if (!"login".equalsIgnoreCase(req.op) && !"cadastrarUsuario".equalsIgnoreCase(req.op)) {
                        String tokenEnviado = (req.op != null && req.op.endsWith("Admin")) ? req.token_admin : req.token;
                        if (tokenSessaoAtiva == null || !tokenSessaoAtiva.equals(tokenEnviado)) {
                            res.resposta = "401"; res.mensagem = "Token inválido ou sessão não autenticada.";
                            out.println(gson.toJson(res));
                            continue;
                        }
                    }

                    if ("login".equalsIgnoreCase(req.op)) {
                        if (usuariosDB.containsKey(req.usuario) && usuariosDB.get(req.usuario).equals(req.senha)) {
                            res.resposta = "200"; 
                            res.token = tokensDB.get(req.usuario); 
                            
                            tokenSessaoAtiva = res.token;
                            usuarioSessaoAtiva = req.usuario;
                            
                            // EP-3: Adiciona o usuário aos "Logados" e atualiza a interface
                            sessoesAtivas.put(usuarioSessaoAtiva, out);
                            atualizarListaOnlineUI();
                        } else { res.resposta = "401"; res.mensagem = "Credenciais invalidas"; }
                    }
                    
                    else if ("ListarUsuariosLogados".equalsIgnoreCase(req.op)) {
                        res.resposta = "200";
                        res.usuarios = new ArrayList<>(sessoesAtivas.keySet());
                    }

                    // EP-3: ROTEADOR DE MENSAGENS PRIVADAS
                    else if ("enviarMensagem".equalsIgnoreCase(req.op)) {
                        if (sessoesAtivas.containsKey(req.destinatario)) {
                            // Cria a mensagem para empurrar (push) para o destino
                            MensagemDTO pushMsg = new MensagemDTO();
                            pushMsg.op = "receberMensagem";
                            pushMsg.remetente = usuarioSessaoAtiva;
                            pushMsg.mensagem = req.mensagem;
                            
                            // Pega a "linha telefônica" do destinatário e envia
                            PrintWriter outDestino = sessoesAtivas.get(req.destinatario);
                            outDestino.println(gson.toJson(pushMsg));
                            
                            res.resposta = "200"; res.mensagem = "Mensagem enviada";
                        } else {
                            res.resposta = "401"; res.mensagem = "Destinatário offline ou inexistente";
                        }
                    }

                    // EP-3: ROTEADOR DE BROADCAST
                    else if ("enviarBroadcast".equalsIgnoreCase(req.op)) {
                        MensagemDTO pushMsg = new MensagemDTO();
                        pushMsg.op = "receberBroadcast";
                        pushMsg.remetente = usuarioSessaoAtiva;
                        pushMsg.mensagem = req.mensagem;
                        
                        String jsonPush = gson.toJson(pushMsg);
                        
                        for (Map.Entry<String, PrintWriter> entrada : sessoesAtivas.entrySet()) {
                            // Envia para todos, exceto para si mesmo
                            if (!entrada.getKey().equals(usuarioSessaoAtiva)) {
                                entrada.getValue().println(jsonPush);
                            }
                        }
                        res.resposta = "200"; res.mensagem = "Mensagem enviada a todos";
                    }

                    else if ("logout".equalsIgnoreCase(req.op)) { 
                        res.resposta = "200";
                        break; // Sai do laço while
                    }

                    // Devolve a resposta síncrona para o cliente que pediu
                    String jsonRes = gson.toJson(res);
                    registrarLog("<- ENVIADO: " + jsonRes);
                    out.println(jsonRes);
                }
            } catch (Exception e) {
                registrarLog("[AVISO] Cliente desconectou abruptamente.");
            } finally {
                // EP-3: Se o cliente fechar o app, remove dos online
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