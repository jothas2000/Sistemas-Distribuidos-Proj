import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.google.gson.Gson;

@SuppressWarnings("unused")
public class ChatClientGUI extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();
    private String meuToken = "", meuUsuario = "";

    private CardLayout cardLayout = new CardLayout();
    private JPanel painelPrincipal = new JPanel(cardLayout);
    
    private JTextPane areaChatPane = new JTextPane();
    private JTextArea areaLogs = new JTextArea();
    private JTabbedPane abasApp = new JTabbedPane();
    private JPanel painelAdmin;
    private JTextField fNovoNome = new JTextField();
    private JPasswordField fNovaSenha = new JPasswordField();

    private JTextField fTokenAdmin = new JTextField("adm", 15);
    private JTextField fTokenUsuario = new JTextField(15); 

    // --- EP-3: Elementos Novos de Interface ---
    private DefaultListModel<String> modeloOnline = new DefaultListModel<>();
    private JList<String> listaOnlineUI = new JList<>(modeloOnline);
    private JComboBox<String> comboDestino = new JComboBox<>();
    private Timer timerAtualizacao;
    private boolean escutandoServidor = false;

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR (EP-3)");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 

        painelPrincipal.add(criarTelaLogin(), "LOGIN");
        painelPrincipal.add(criarTelaApp(), "APP");
        add(painelPrincipal);
    }

    private boolean conectar(String ip, int porta) {
        try {
            socket = new Socket(ip, porta);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            return true;
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Erro ao conectar: Servidor Offline ou IP/Porta incorretos.", "Falha", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JPanel criarTelaLogin() {
        JPanel painelFundo = new JPanel(new GridBagLayout());
        JPanel caixaLogin = new JPanel(new GridBagLayout());
        caixaLogin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(20, 30, 20, 30)));
        caixaLogin.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Acesso ao Sistema", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        caixaLogin.add(titulo, g);

        g.gridwidth = 1;
        JTextField fIP = new JTextField("127.0.0.1", 15);
        JTextField fPorta = new JTextField("8080", 15);
        JTextField fNome = new JTextField(15); 
        JTextField fUser = new JTextField(15); 
        JPasswordField fPass = new JPasswordField(15);
        
        g.gridy = 1; g.gridx = 0; caixaLogin.add(new JLabel("IP Servidor:"), g);
        g.gridx = 1; caixaLogin.add(fIP, g);
        g.gridy = 2; g.gridx = 0; caixaLogin.add(new JLabel("Porta:"), g);
        g.gridx = 1; caixaLogin.add(fPorta, g);
        g.gridy = 3; g.gridx = 0; caixaLogin.add(new JLabel("Nome:"), g);
        g.gridx = 1; caixaLogin.add(fNome, g);
        g.gridy = 4; g.gridx = 0; caixaLogin.add(new JLabel("Usuário (Login):"), g);
        g.gridx = 1; caixaLogin.add(fUser, g);
        g.gridy = 5; g.gridx = 0; caixaLogin.add(new JLabel("Senha:"), g);
        g.gridx = 1; caixaLogin.add(fPass, g);

        JButton bLogin = new JButton("Entrar"); 
        bLogin.setBackground(new Color(70, 130, 180)); bLogin.setForeground(Color.WHITE);
        JButton bCad = new JButton("Cadastrar");
        bCad.setBackground(new Color(40, 167, 69)); bCad.setForeground(Color.WHITE);

        JPanel pBotoes = new JPanel(new GridLayout(1, 2, 10, 0));
        pBotoes.setOpaque(false);
        pBotoes.add(bLogin); pBotoes.add(bCad);
        
        g.gridy = 6; g.gridx = 0; g.gridwidth = 2;
        caixaLogin.add(pBotoes, g);
        painelFundo.add(caixaLogin);

        bLogin.addActionListener(e -> {
            String u = fUser.getText().trim();
            String s = new String(fPass.getPassword()).trim();
            
            if(u.isEmpty() || s.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Usuário e senha vazios!", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO req = new MensagemDTO(); req.op = "login"; req.usuario = u; req.senha = s;
                MensagemDTO res = enviarDadosSincrono(req); // Login ainda é síncrono
                
                if (res != null && "200".equals(res.resposta)) {
                    meuUsuario = u; meuToken = res.token;
                    fTokenUsuario.setText(meuToken); 
                    
                    configurarAbas(); 
                    iniciarThreadReceptora(); // INICIA A ESCUTA ASSÍNCRONA EP-3
                    iniciarPollingUsuarios(); // Inicia atualização da lista de online
                    cardLayout.show(painelPrincipal, "APP");
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de conexão", "Falha", JOptionPane.ERROR_MESSAGE);
                    try { socket.close(); } catch (Exception ex) {}
                }
            }
        });
        
        bCad.addActionListener(e -> {
            String n = fNome.getText().trim();
            String u = fUser.getText().trim();
            String s = new String(fPass.getPassword()).trim();
            
            if(n.isEmpty() || u.isEmpty() || s.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha Nome, Usuário e Senha para cadastrar!", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO req = new MensagemDTO(); req.op = "cadastrarUsuario"; req.usuario = u; req.nome = n; req.senha = s;
                MensagemDTO res = enviarDadosSincrono(req);
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                try { socket.close(); } catch (Exception ex) {}
            }
        });

        return painelFundo;
    }

    private void configurarAbas() {
        if ("adm".equals(meuToken)) {
            if (abasApp.indexOfTab("Painel Admin") == -1) {
                painelAdmin = criarPainelAdmin();
                abasApp.addTab("Painel Admin", painelAdmin);
            }
        } else {
            int index = abasApp.indexOfTab("Painel Admin");
            if (index != -1) abasApp.removeTabAt(index);
        }
    }

    private Container criarTelaApp() {
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPrincipal.setDividerLocation(600);
        
        // --- CHAT COM LISTA DE ONLINE (EP-3) ---
        JPanel pChatContainer = new JPanel(new BorderLayout());
        areaChatPane.setEditable(false);
        areaChatPane.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JPanel painelOnline = new JPanel(new BorderLayout());
        painelOnline.setPreferredSize(new Dimension(150, 0));
        painelOnline.setBorder(BorderFactory.createTitledBorder("Logados"));
        painelOnline.add(new JScrollPane(listaOnlineUI), BorderLayout.CENTER);
        
        JSplitPane splitChat = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(areaChatPane), painelOnline);
        splitChat.setResizeWeight(0.8);
        pChatContainer.add(splitChat, BorderLayout.CENTER);

        // --- PAINEL DE ENVIO EP-3 ---
        JTextField tMsg = new JTextField(); 
        JButton bEnv = new JButton("Enviar Msg");
        
        comboDestino.addItem("todos"); 
        
        JPanel pEnvio = new JPanel(new BorderLayout(5, 5));
        pEnvio.setBorder(new EmptyBorder(5, 5, 5, 5));
        pEnvio.add(new JLabel("Para:"), BorderLayout.WEST);
        pEnvio.add(comboDestino, BorderLayout.CENTER);
        
        JPanel pInputMsg = new JPanel(new BorderLayout(5, 5));
        pInputMsg.add(tMsg, BorderLayout.CENTER);
        pInputMsg.add(bEnv, BorderLayout.EAST);
        
        JPanel pSulChat = new JPanel(new BorderLayout());
        pSulChat.add(pEnvio, BorderLayout.NORTH);
        pSulChat.add(pInputMsg, BorderLayout.CENTER);
        
        pChatContainer.add(pSulChat, BorderLayout.SOUTH);
        // -----------------------------

        abasApp.addTab("Chat Geral", pChatContainer);
        abasApp.addTab("Perfil/Config", criarPainelConfiguracoes()); 

        JPanel pLogs = new JPanel(new BorderLayout());
        areaLogs.setBackground(Color.BLACK); areaLogs.setForeground(Color.GREEN);
        areaLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLogs.setEditable(false);
        
        JButton bOut = new JButton("Logout (Voltar)"), bBye = new JButton("Sair do App");
        JPanel pBotoesSair = new JPanel(new GridLayout(2,1)); 
        pBotoesSair.add(bOut); pBotoesSair.add(bBye);
        
        pLogs.add(new JScrollPane(areaLogs), BorderLayout.CENTER); 
        pLogs.add(pBotoesSair, BorderLayout.SOUTH);

        splitPrincipal.setLeftComponent(abasApp); splitPrincipal.setRightComponent(pLogs);

        // AÇÃO ENVIAR MENSAGEM (EP-3 - Usa broadcast ou unicast)
        bEnv.addActionListener(e -> { 
            if(!tMsg.getText().trim().isEmpty()) {
                String dest = comboDestino.getSelectedItem().toString();
                MensagemDTO req = new MensagemDTO();
                req.op = "todos".equals(dest) ? "enviarBroadcast" : "enviarMensagem";
                req.token = meuToken;
                req.destinatario = "todos".equals(dest) ? null : dest;
                req.mensagem = tMsg.getText();
                
                enviarDadosAssincrono(req);
                tMsg.setText(""); 
            }
        });

        bOut.addActionListener(e -> {
            MensagemDTO req = new MensagemDTO(); req.op = "logout"; req.token = meuToken;
            enviarDadosAssincrono(req);
            fecharSessao();
        });
        
        bBye.addActionListener(e -> { 
            MensagemDTO req = new MensagemDTO(); req.op = "logout"; req.token = meuToken;
            enviarDadosAssincrono(req);
            System.exit(0); 
        });

        return splitPrincipal;
    }

    // =========================================================================
    // ======== MÓDULO ASSÍNCRONO E THREADS (EP-3) =============================
    // =========================================================================

    private void iniciarThreadReceptora() {
        escutandoServidor = true;
        new Thread(() -> {
            try {
                String linha;
                while (escutandoServidor && (linha = in.readLine()) != null) {
                    final String rawJson = linha;
                    SwingUtilities.invokeLater(() -> processarChegadaDeDados(rawJson));
                }
            } catch (Exception e) {
                if (escutandoServidor) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Conexão perdida com o servidor.", "Desconectado", JOptionPane.WARNING_MESSAGE));
                    fecharSessao();
                }
            }
        }).start();
    }

    private void processarChegadaDeDados(String jsonResponse) {
        areaLogs.append("<- " + jsonResponse + "\n\n"); 
        areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        
        MensagemDTO res = gson.fromJson(jsonResponse, MensagemDTO.class);

        // Chegada de Mensagens no Chat
        if ("receberMensagem".equals(res.op)) {
            areaChatPane.setText(areaChatPane.getText() + "\n[PRIVADO] " + res.remetente + " diz: " + res.mensagem);
        } 
        else if ("receberBroadcast".equals(res.op)) {
            areaChatPane.setText(areaChatPane.getText() + "\n[BROADCAST] " + res.remetente + " diz: " + res.mensagem);
        }
        // Atualização da Lista de Utilizadores
        else if (res.usuarios != null && "200".equals(res.resposta)) {
            String destinoAtual = (String) comboDestino.getSelectedItem();
            comboDestino.removeAllItems();
            comboDestino.addItem("todos");
            modeloOnline.clear();
            
            for (String u : res.usuarios) {
                modeloOnline.addElement(u);
                if (!u.equals(meuUsuario)) comboDestino.addItem(u);
            }
            if (destinoAtual != null && res.usuarios.contains(destinoAtual)) {
                comboDestino.setSelectedItem(destinoAtual);
            }
        }
        // Retorno da lista de Administrador
        else if (res.lista_usuarios != null && "200".equals(res.resposta)) {
            StringBuilder sb = new StringBuilder("=== USUÁRIOS NO SISTEMA ===\n\n");
            for(Map<String, String> userMap : res.lista_usuarios) {
                String u = "", n = "";
                for (String key : userMap.keySet()) {
                    if (key.startsWith("usuario")) u = userMap.get(key);
                    if (key.startsWith("nome")) n = userMap.get(key);
                }
                sb.append("Login: ").append(u).append("  |  Nome: ").append(n).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Lista de Usuários", JOptionPane.INFORMATION_MESSAGE);
        }
        // Mensagens de Sucesso ou Erro Crítico (Alertas)
        else if (res.mensagem != null) {
            if ("401".equals(res.resposta)) {
                JOptionPane.showMessageDialog(this, res.mensagem, "Aviso de Segurança / Erro", JOptionPane.WARNING_MESSAGE);
            } else if ("200".equals(res.resposta) && !res.mensagem.contains("Mensagem enviada")) {
                JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void iniciarPollingUsuarios() {
        if (timerAtualizacao != null) timerAtualizacao.stop();
        timerAtualizacao = new Timer(5000, e -> {
            if (escutandoServidor) {
                MensagemDTO req = new MensagemDTO(); req.op = "ListarUsuariosLogados"; req.token = meuToken;
                enviarDadosAssincrono(req);
            }
        });
        timerAtualizacao.start();
    }

    private void fecharSessao() {
        escutandoServidor = false;
        if (timerAtualizacao != null) timerAtualizacao.stop();
        try { socket.close(); } catch (Exception ex) {}
        areaLogs.setText(""); areaChatPane.setText("");
        cardLayout.show(painelPrincipal, "LOGIN");
    }

    // Envio antes de iniciar a Thread (Usado no Login e Cadastro)
    private MensagemDTO enviarDadosSincrono(MensagemDTO req) {
        try {
            String jsonRequest = gson.toJson(req); 
            out.println(jsonRequest);
            String jsonResponse = in.readLine(); 
            return gson.fromJson(jsonResponse, MensagemDTO.class);
        } catch (Exception e) { return null; }
    }

    // Envio enquanto o Chat está aberto (Não espera resposta, a Thread que lê)
    private void enviarDadosAssincrono(MensagemDTO req) {
        try {
            String jsonRequest = gson.toJson(req); 
            out.println(jsonRequest);
            areaLogs.append("-> " + jsonRequest + "\n"); 
            areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        } catch (Exception e) { }
    }

    // =========================================================================
    // ======== ABAS SECUNDÁRIAS (ADMIN E CONFIGURAÇÕES) =======================
    // =========================================================================

    private JPanel criarPainelAdmin() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8); 
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        g.gridx = 0; g.gridy = y; p.add(new JLabel("Modificar Token Admin (Teste):"), g);
        g.gridx = 1; g.gridwidth = 2; p.add(fTokenAdmin, g);
        g.gridwidth = 1; y++;

        p.add(new JSeparator(), g); y++;

        JButton bListar = new JButton("Listar Todos os Usuários (Console/Pop-Up)");
        bListar.setBackground(new Color(70, 130, 180)); bListar.setForeground(Color.WHITE);
        g.gridx = 0; g.gridy = y; g.gridwidth = 3; p.add(bListar, g);
        g.gridwidth = 1; y++;

        p.add(new JSeparator(), g); y++;

        JTextField fConsUser = new JTextField(15);
        JButton bCons = new JButton("Buscar Dados");
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Consultar (Login alvo):"), g);
        g.gridx = 1; p.add(fConsUser, g);
        g.gridx = 2; p.add(bCons, g); y++;

        p.add(new JSeparator(), g); y++;

        JTextField fAtuUser = new JTextField(15);
        JTextField fAtuNome = new JTextField(15);
        JTextField fAtuSenha = new JTextField(15);
        JButton bAtu = new JButton("Forçar Atualização");
        
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Login alvo para alterar:"), g);
        g.gridx = 1; g.gridwidth = 2; p.add(fAtuUser, g); g.gridwidth = 1; y++;
        
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Novo Nome (vazio p/ ignorar):"), g);
        g.gridx = 1; g.gridwidth = 2; p.add(fAtuNome, g); g.gridwidth = 1; y++;
        
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Nova Senha (vazio p/ ignorar):"), g);
        g.gridx = 1; p.add(fAtuSenha, g); 
        g.gridx = 2; p.add(bAtu, g); y++;

        p.add(new JSeparator(), g); y++;

        JTextField fDelUser = new JTextField(15);
        JButton bDel = new JButton("Apagar Conta");
        bDel.setBackground(Color.RED); bDel.setForeground(Color.WHITE);
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Login alvo para excluir:"), g);
        g.gridx = 1; p.add(fDelUser, g);
        g.gridx = 2; p.add(bDel, g); y++;

        bListar.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuariosAdmin"; m.token_admin = fTokenAdmin.getText().trim();
            enviarDadosAssincrono(m); // A resposta aciona o JOptionPane lá na Thread
        });

        bCons.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fConsUser.getText().trim();
            enviarDadosAssincrono(m);
        });

        bAtu.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "atualizarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fAtuUser.getText().trim();
            m.nome = fAtuNome.getText().trim().isEmpty() ? null : fAtuNome.getText().trim(); 
            m.senha = fAtuSenha.getText().trim().isEmpty() ? null : fAtuSenha.getText().trim();
            enviarDadosAssincrono(m);
        });

        bDel.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "deletarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fDelUser.getText().trim();
            enviarDadosAssincrono(m);
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(new JScrollPane(p), BorderLayout.CENTER);
        return wrap;
    }

    private JPanel criarPainelConfiguracoes() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        g.gridx = 0; g.gridy = y; g.gridwidth = 2;
        JLabel titulo = new JLabel("Atualizar Cadastro / Segurança", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(titulo, g); y++;

        g.gridwidth = 1; 
        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Modificar Token Usuário (Teste):"), g);
        g.gridx = 1; p.add(fTokenUsuario, g); y++;

        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Novo Nome (vazio p/ ignorar):"), g);
        g.gridx = 1; fNovoNome.setColumns(15); p.add(fNovoNome, g); y++;

        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Nova Senha (vazio p/ ignorar):"), g);
        g.gridx = 1; fNovaSenha.setColumns(15); p.add(fNovaSenha, g); y++;

        g.gridy = y; g.gridx = 0; g.gridwidth = 2;
        JButton bSalvar = new JButton("Salvar Alterações");
        bSalvar.setBackground(new Color(40, 167, 69)); bSalvar.setForeground(Color.WHITE);
        bSalvar.addActionListener(e -> executarAtualizacao());
        p.add(bSalvar, g); y++;
        
        JButton bDel = new JButton("Apagar Minha Conta Permanentemente");
        bDel.setBackground(Color.RED); bDel.setForeground(Color.WHITE);
        g.gridy = y; p.add(bDel, g);

        bDel.addActionListener(e -> {
            int confirma = JOptionPane.showConfirmDialog(this, "Tem certeza?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
            if (confirma == JOptionPane.YES_OPTION) {
                MensagemDTO req = new MensagemDTO(); req.op = "deletarUsuario"; req.token = meuToken;
                enviarDadosAssincrono(req);
                fecharSessao();
            }
        });

        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(new EmptyBorder(30, 30, 30, 30));
        container.add(p, BorderLayout.NORTH);
        return container;
    }

    private void executarAtualizacao() {
        String novoNome = fNovoNome.getText().trim();
        String novaSenha = new String(fNovaSenha.getPassword()).trim();

        if (novoNome.isEmpty() && novaSenha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha pelo menos um campo para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MensagemDTO req = new MensagemDTO(); req.op = "atualizarUsuario";
        req.token = fTokenUsuario.getText().trim(); 
        req.nome = novoNome.isEmpty() ? null : novoNome;   
        req.senha = novaSenha.isEmpty() ? null : novaSenha; 

        enviarDadosAssincrono(req);
        fNovoNome.setText(""); fNovaSenha.setText("");
    }

    public static void main(String[] args) { 
        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true)); 
    }
}