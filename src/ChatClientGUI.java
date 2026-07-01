import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Hashtable;
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
    
    private JTextArea areaLogs = new JTextArea();
    private JTabbedPane abasApp = new JTabbedPane();
    private JPanel painelAdmin;
    private JTextField fNovoNome = new JTextField();
    private JPasswordField fNovaSenha = new JPasswordField();
    private JTextField fTokenAdmin = new JTextField("adm", 15);
    private JTextField fTokenUsuario = new JTextField(15); 

    private DefaultListModel<String> modeloOnline = new DefaultListModel<>();
    private JList<String> listaOnlineUI = new JList<>(modeloOnline);
    
    private CardLayout layoutChats = new CardLayout();
    private JPanel containerChats = new JPanel(layoutChats);
    private Map<String, JTextPane> paineisDeTexto = new HashMap<>();
    
    private Timer timerAtualizacao;
    private boolean escutandoServidor = false;

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR (EP-3 GUI Moderna)");
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
        
        g.gridy = 1; g.gridx = 0; caixaLogin.add(new JLabel("IP Servidor:"), g); g.gridx = 1; caixaLogin.add(fIP, g);
        g.gridy = 2; g.gridx = 0; caixaLogin.add(new JLabel("Porta:"), g); g.gridx = 1; caixaLogin.add(fPorta, g);
        g.gridy = 3; g.gridx = 0; caixaLogin.add(new JLabel("Nome:"), g); g.gridx = 1; caixaLogin.add(fNome, g);
        g.gridy = 4; g.gridx = 0; caixaLogin.add(new JLabel("Usuário (Login):"), g); g.gridx = 1; caixaLogin.add(fUser, g);
        g.gridy = 5; g.gridx = 0; caixaLogin.add(new JLabel("Senha:"), g); g.gridx = 1; caixaLogin.add(fPass, g);

        JButton bLogin = new JButton("Entrar"); bLogin.setBackground(new Color(70, 130, 180)); bLogin.setForeground(Color.WHITE);
        JButton bCad = new JButton("Cadastrar"); bCad.setBackground(new Color(40, 167, 69)); bCad.setForeground(Color.WHITE);

        JPanel pBotoes = new JPanel(new GridLayout(1, 2, 10, 0)); pBotoes.setOpaque(false);
        pBotoes.add(bLogin); pBotoes.add(bCad);
        
        g.gridy = 6; g.gridx = 0; g.gridwidth = 2; caixaLogin.add(pBotoes, g); painelFundo.add(caixaLogin);

        bLogin.addActionListener(e -> {
            String u = fUser.getText().trim(); String s = new String(fPass.getPassword()).trim();
            if(u.isEmpty() || s.isEmpty()) { JOptionPane.showMessageDialog(this, "Usuário e senha vazios!", "Erro", JOptionPane.WARNING_MESSAGE); return; }

            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO req = new MensagemDTO(); req.op = "login"; req.usuario = u; req.senha = s;
                MensagemDTO res = enviarDadosSincrono(req);
                
                if (res != null && "200".equals(res.resposta)) {
                    meuUsuario = u; meuToken = res.token; fTokenUsuario.setText(meuToken); 
                    configurarAbas(); iniciarAmbienteChat(); iniciarThreadReceptora(); iniciarPollingUsuarios(); 
                    cardLayout.show(painelPrincipal, "APP");
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de conexão", "Falha", JOptionPane.ERROR_MESSAGE);
                    try { socket.close(); } catch (Exception ex) {}
                }
            }
        });
        
        bCad.addActionListener(e -> {
            String n = fNome.getText().trim(); String u = fUser.getText().trim(); String s = new String(fPass.getPassword()).trim();
            if(n.isEmpty() || u.isEmpty() || s.isEmpty()) { JOptionPane.showMessageDialog(this, "Preencha tudo!", "Erro", JOptionPane.WARNING_MESSAGE); return; }
            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO req = new MensagemDTO(); req.op = "cadastrarUsuario"; req.usuario = u; req.nome = n; req.senha = s;
                MensagemDTO res = enviarDadosSincrono(req); JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                try { socket.close(); } catch (Exception ex) {}
            }
        });

        return painelFundo;
    }

    private void configurarAbas() {
        if ("adm".equals(meuToken)) { if (abasApp.indexOfTab("Painel Admin") == -1) { painelAdmin = criarPainelAdmin(); abasApp.addTab("Painel Admin", painelAdmin); }
        } else { int index = abasApp.indexOfTab("Painel Admin"); if (index != -1) abasApp.removeTabAt(index); }
    }

    private void iniciarAmbienteChat() {
        paineisDeTexto.clear(); containerChats.removeAll(); modeloOnline.clear();
    }

    private JPanel criarPainelConversa(String alvo) {
        JPanel painel = new JPanel(new BorderLayout());
        
        JTextPane areaDeTexto = new JTextPane(); areaDeTexto.setEditable(false); areaDeTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        paineisDeTexto.put(alvo, areaDeTexto); 
        
        JTextField inputTexto = new JTextField(); JButton btnEnviar = new JButton("Enviar");
        
        btnEnviar.addActionListener(e -> {
            String msg = inputTexto.getText().trim();
            if(!msg.isEmpty()) {
                MensagemDTO req = new MensagemDTO(); req.token = meuToken; req.mensagem = msg; req.op = "enviarMensagem"; req.destinatario = alvo;   
                adicionarMensagemChat(alvo, "[Você]: " + msg); enviarDadosAssincrono(req); inputTexto.setText("");
            }
        });
        
        JPanel pSul = new JPanel(new BorderLayout(5, 5)); pSul.setBorder(new EmptyBorder(5, 5, 5, 5));
        pSul.add(inputTexto, BorderLayout.CENTER); pSul.add(btnEnviar, BorderLayout.EAST);
        
        JLabel titulo = new JLabel("Conversando com: " + alvo, SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14)); titulo.setBorder(new EmptyBorder(5, 0, 5, 0));
        titulo.setOpaque(true); titulo.setBackground(new Color(230, 230, 230));

        painel.add(titulo, BorderLayout.NORTH); painel.add(new JScrollPane(areaDeTexto), BorderLayout.CENTER); painel.add(pSul, BorderLayout.SOUTH);
        return painel;
    }

    private Container criarTelaApp() {
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPrincipal.setDividerLocation(650); // Aumentei um pouco o lado do chat
        
        // ==== LADO ESQUERDO: LISTA E CONTROLES ====
        listaOnlineUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaOnlineUI.setFont(new Font("Arial", Font.BOLD, 14));
        listaOnlineUI.setFixedCellHeight(30);
        
        JPanel painelOnline = new JPanel(new BorderLayout());
        painelOnline.setPreferredSize(new Dimension(190, 0)); // Aumentado para caber o slider
        painelOnline.setBorder(BorderFactory.createTitledBorder("Logados"));
        painelOnline.add(new JScrollPane(listaOnlineUI), BorderLayout.CENTER);
        
        // ======== NOVO PAINEL DE CONTROLES SUL ========
        JPanel painelControles = new JPanel(new GridLayout(2, 1, 0, 5));
        painelControles.setBorder(new EmptyBorder(5, 0, 0, 0));

        // 1. O Toggle Auto/Manual
        JPanel painelSlider = new JPanel(new BorderLayout(5, 0));
        
        JSlider sliderModo = new JSlider(0, 1, 1); // 0 = Manual (Esq), 1 = Auto (Dir)
        sliderModo.setSnapToTicks(true);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(0, new JLabel("Manual"));
        labels.put(1, new JLabel("Auto"));
        sliderModo.setLabelTable(labels);
        sliderModo.setPaintLabels(true);
        
        JButton btnAtualizar = new JButton("🔄");
        btnAtualizar.setEnabled(false); // Começa falso porque o slider tá na direita (Auto)
        btnAtualizar.setToolTipText("Pedir Lista Agora");
        
        painelSlider.add(sliderModo, BorderLayout.CENTER);
        painelSlider.add(btnAtualizar, BorderLayout.EAST);
        
        // Ações do Slider
        sliderModo.addChangeListener(e -> {
            if (sliderModo.getValue() == 1) { // DIREITA = AUTO
                btnAtualizar.setEnabled(false);
                iniciarPollingUsuarios();
            } else { // ESQUERDA = MANUAL
                btnAtualizar.setEnabled(true);
                if (timerAtualizacao != null) timerAtualizacao.stop();
            }
        });
        
        // Ação do Botão Manual
        btnAtualizar.addActionListener(e -> {
            MensagemDTO req = new MensagemDTO(); req.op = "listarUsuariosLogados"; req.token = meuToken;
            enviarDadosAssincrono(req);
        });

        // 2. Botão de Broadcast
        JButton btnBroadcast = new JButton("📢 Enviar Broadcast");
        btnBroadcast.setBackground(new Color(255, 193, 7)); 
        btnBroadcast.setFont(new Font("Arial", Font.BOLD, 12));
        btnBroadcast.setFocusPainted(false);
        btnBroadcast.addActionListener(e -> dispararBroadcastDialog());
        
        painelControles.add(painelSlider);
        painelControles.add(btnBroadcast);
        
        painelOnline.add(painelControles, BorderLayout.SOUTH);
        
        // ==============================================
        
        listaOnlineUI.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String alvoSelecionado = listaOnlineUI.getSelectedValue();
                if (alvoSelecionado != null) {
                    if (!paineisDeTexto.containsKey(alvoSelecionado)) {
                        containerChats.add(criarPainelConversa(alvoSelecionado), alvoSelecionado);
                    }
                    layoutChats.show(containerChats, alvoSelecionado);
                }
            }
        });

        JSplitPane splitChat = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelOnline, containerChats);
        splitChat.setDividerLocation(190);

        abasApp.addTab("Mensagens", splitChat);
        abasApp.addTab("Perfil/Config", criarPainelConfiguracoes()); 

        JPanel pLogs = new JPanel(new BorderLayout());
        areaLogs.setBackground(Color.BLACK); areaLogs.setForeground(Color.GREEN);
        areaLogs.setFont(new Font("Monospaced", Font.PLAIN, 12)); areaLogs.setEditable(false);
        
        JButton bOut = new JButton("Logout (Voltar)"), bBye = new JButton("Sair do App");
        JPanel pBotoesSair = new JPanel(new GridLayout(2,1)); pBotoesSair.add(bOut); pBotoesSair.add(bBye);
        
        pLogs.add(new JScrollPane(areaLogs), BorderLayout.CENTER); pLogs.add(pBotoesSair, BorderLayout.SOUTH);

        splitPrincipal.setLeftComponent(abasApp); splitPrincipal.setRightComponent(pLogs);

        bOut.addActionListener(e -> { MensagemDTO req = new MensagemDTO(); req.op = "logout"; req.token = meuToken; enviarDadosAssincrono(req); fecharSessao(); });
        bBye.addActionListener(e -> { MensagemDTO req = new MensagemDTO(); req.op = "logout"; req.token = meuToken; enviarDadosAssincrono(req); System.exit(0); });

        return splitPrincipal;
    }

    private void dispararBroadcastDialog() {
        String msg = JOptionPane.showInputDialog(this, "Digite a mensagem de Broadcast (para todos):", "Aviso Global", JOptionPane.PLAIN_MESSAGE);
        
        if (msg != null && !msg.trim().isEmpty()) {
            MensagemDTO req = new MensagemDTO(); req.token = meuToken; req.mensagem = msg.trim(); req.op = "enviarMensagem"; req.destinatario = "/todos"; 
            
            if (!paineisDeTexto.isEmpty()) {
                String msgFormatada = "[Seu Broadcast]: " + req.mensagem;
                for (String sala : paineisDeTexto.keySet()) { adicionarMensagemChat(sala, msgFormatada); }
                enviarDadosAssincrono(req);
            } else { JOptionPane.showMessageDialog(this, "Não há nenhum outro usuário online para receber.", "Aviso", JOptionPane.WARNING_MESSAGE); }
        }
    }

    private void adicionarMensagemChat(String alvo, String texto) {
        if (!paineisDeTexto.containsKey(alvo)) { containerChats.add(criarPainelConversa(alvo), alvo); }
        JTextPane area = paineisDeTexto.get(alvo);
        try { Document doc = area.getDocument(); doc.insertString(doc.getLength(), texto + "\n", null); area.setCaretPosition(doc.getLength()); } catch (Exception e) {}
    }

    private void iniciarThreadReceptora() {
        escutandoServidor = true;
        new Thread(() -> {
            try {
                String linha;
                while (escutandoServidor && (linha = in.readLine()) != null) {
                    final String rawJson = linha; SwingUtilities.invokeLater(() -> processarChegadaDeDados(rawJson));
                }
            } catch (Exception e) {
                if (escutandoServidor) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Desconectado.", "Aviso", JOptionPane.WARNING_MESSAGE)); fecharSessao(); }
            }
        }).start();
    }

    private void processarChegadaDeDados(String jsonResponse) {
        areaLogs.append("<- " + jsonResponse + "\n\n"); 
        areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        MensagemDTO res = gson.fromJson(jsonResponse, MensagemDTO.class);

        // 1. TRATAMENTO DE MENSAGENS E BROADCAST
        if ("enviarMensagem".equals(res.op) || "receberMensagem".equals(res.op)) {
            boolean isBroadcast = (res.destinatario != null && (res.destinatario.equals("/todos") || res.destinatario.equals("todos")));
            if (isBroadcast) {
                String msgFormatada = "[BROADCAST de " + res.remetente + "]: " + res.mensagem;
                for (String sala : paineisDeTexto.keySet()) { adicionarMensagemChat(sala, msgFormatada); }
            } else { adicionarMensagemChat(res.remetente, "[" + res.remetente + "]: " + res.mensagem); }
        }
        
        // 2. LISTAS
        boolean isListaOnline = false; java.util.List<String> listagemLogados = new java.util.ArrayList<>();
        if (res.usuarios != null) { isListaOnline = true; listagemLogados = res.usuarios; } 
        else if (res.lista_usuarios != null) {
            if (res.lista_usuarios.isEmpty() || res.lista_usuarios.get(0) instanceof String) {
                isListaOnline = true; for (Object obj : res.lista_usuarios) { listagemLogados.add(String.valueOf(obj)); }
            }
        }

        if (isListaOnline && "200".equals(res.resposta)) {
            String selecionadoAtual = listaOnlineUI.getSelectedValue(); modeloOnline.clear();
            for (String u : listagemLogados) {
                if (!u.equals(meuUsuario)) {
                    modeloOnline.addElement(u);
                    if (!paineisDeTexto.containsKey(u)) { containerChats.add(criarPainelConversa(u), u); }
                }
            }
            if (selecionadoAtual != null && modeloOnline.contains(selecionadoAtual)) { listaOnlineUI.setSelectedValue(selecionadoAtual, true); } 
            else if (modeloOnline.size() > 0) { listaOnlineUI.setSelectedIndex(0); }
        }
        else if (res.lista_usuarios != null && !res.lista_usuarios.isEmpty() && res.lista_usuarios.get(0) instanceof java.util.Map && "200".equals(res.resposta)) {
            StringBuilder sb = new StringBuilder("=== USUÁRIOS NO SISTEMA ===\n\n");
            for(Object obj : res.lista_usuarios) {
                java.util.Map<?, ?> userMap = (java.util.Map<?, ?>) obj; String u = "", n = "";
                for (Object key : userMap.keySet()) {
                    if (key.toString().startsWith("usuario")) u = userMap.get(key).toString();
                    if (key.toString().startsWith("nome")) n = userMap.get(key).toString();
                }
                sb.append("Login: ").append(u).append("  |  Nome: ").append(n).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Lista", JOptionPane.INFORMATION_MESSAGE);
        }
        else if (res.mensagem != null) {
            if ("401".equals(res.resposta)) { JOptionPane.showMessageDialog(this, res.mensagem, "Aviso", JOptionPane.WARNING_MESSAGE); } 
            else if ("200".equals(res.resposta) && !res.mensagem.contains("Mensagem enviada")) { JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE); }
        }
    }

    private void iniciarPollingUsuarios() {
        if (timerAtualizacao != null) timerAtualizacao.stop();
        timerAtualizacao = new Timer(20000, e -> {
            if (escutandoServidor) {
                MensagemDTO req = new MensagemDTO(); req.op = "listarUsuariosLogados"; req.token = meuToken; enviarDadosAssincrono(req);
            }
        });
        timerAtualizacao.start();
    }

    private void fecharSessao() {
        escutandoServidor = false;
        if (timerAtualizacao != null) timerAtualizacao.stop();
        try { socket.close(); } catch (Exception ex) {}
        areaLogs.setText(""); cardLayout.show(painelPrincipal, "LOGIN");
    }

    private MensagemDTO enviarDadosSincrono(MensagemDTO req) {
        try { String jsonRequest = gson.toJson(req); out.println(jsonRequest); String jsonResponse = in.readLine(); return gson.fromJson(jsonResponse, MensagemDTO.class);
        } catch (Exception e) { return null; }
    }

    private void enviarDadosAssincrono(MensagemDTO req) {
        try { String jsonRequest = gson.toJson(req); out.println(jsonRequest); areaLogs.append("-> " + jsonRequest + "\n"); areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        } catch (Exception e) { }
    }

    private JPanel criarPainelAdmin() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(Color.WHITE); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(8, 8, 8, 8); g.fill = GridBagConstraints.HORIZONTAL;
        int y = 0; g.gridx = 0; g.gridy = y; p.add(new JLabel("Modificar Token Admin (Teste):"), g); g.gridx = 1; g.gridwidth = 2; p.add(fTokenAdmin, g); g.gridwidth = 1; y++; p.add(new JSeparator(), g); y++;
        JButton bListar = new JButton("Listar Todos os Usuários (Console/Pop-Up)"); bListar.setBackground(new Color(70, 130, 180)); bListar.setForeground(Color.WHITE); g.gridx = 0; g.gridy = y; g.gridwidth = 3; p.add(bListar, g); g.gridwidth = 1; y++; p.add(new JSeparator(), g); y++;
        JTextField fConsUser = new JTextField(15); JButton bCons = new JButton("Buscar Dados"); g.gridx = 0; g.gridy = y; p.add(new JLabel("Consultar (Login alvo):"), g); g.gridx = 1; p.add(fConsUser, g); g.gridx = 2; p.add(bCons, g); y++; p.add(new JSeparator(), g); y++;
        JTextField fAtuUser = new JTextField(15); JTextField fAtuNome = new JTextField(15); JTextField fAtuSenha = new JTextField(15); JButton bAtu = new JButton("Forçar Atualização");
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Login alvo para alterar:"), g); g.gridx = 1; g.gridwidth = 2; p.add(fAtuUser, g); g.gridwidth = 1; y++;
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Novo Nome (vazio p/ ignorar):"), g); g.gridx = 1; g.gridwidth = 2; p.add(fAtuNome, g); g.gridwidth = 1; y++;
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Nova Senha (vazio p/ ignorar):"), g); g.gridx = 1; p.add(fAtuSenha, g); g.gridx = 2; p.add(bAtu, g); y++; p.add(new JSeparator(), g); y++;
        JTextField fDelUser = new JTextField(15); JButton bDel = new JButton("Apagar Conta"); bDel.setBackground(Color.RED); bDel.setForeground(Color.WHITE);
        g.gridx = 0; g.gridy = y; p.add(new JLabel("Login alvo para excluir:"), g); g.gridx = 1; p.add(fDelUser, g); g.gridx = 2; p.add(bDel, g); y++;
        bListar.addActionListener(e -> { MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuariosAdmin"; m.token_admin = fTokenAdmin.getText().trim(); enviarDadosAssincrono(m); });
        bCons.addActionListener(e -> { MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuarioAdmin"; m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fConsUser.getText().trim(); enviarDadosAssincrono(m); });
        bAtu.addActionListener(e -> { MensagemDTO m = new MensagemDTO(); m.op = "atualizarUsuarioAdmin"; m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fAtuUser.getText().trim(); m.nome = fAtuNome.getText().trim().isEmpty() ? null : fAtuNome.getText().trim(); m.senha = fAtuSenha.getText().trim().isEmpty() ? null : fAtuSenha.getText().trim(); enviarDadosAssincrono(m); });
        bDel.addActionListener(e -> { MensagemDTO m = new MensagemDTO(); m.op = "deletarUsuarioAdmin"; m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fDelUser.getText().trim(); enviarDadosAssincrono(m); });
        JPanel wrap = new JPanel(new BorderLayout()); wrap.add(new JScrollPane(p), BorderLayout.CENTER); return wrap;
    }

    private JPanel criarPainelConfiguracoes() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(Color.WHITE); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;
        int y = 0; g.gridx = 0; g.gridy = y; g.gridwidth = 2; JLabel titulo = new JLabel("Atualizar Cadastro / Segurança", SwingConstants.CENTER); titulo.setFont(new Font("Arial", Font.BOLD, 16)); p.add(titulo, g); y++;
        g.gridwidth = 1; g.gridy = y; g.gridx = 0; p.add(new JLabel("Modificar Token Usuário (Teste):"), g); g.gridx = 1; p.add(fTokenUsuario, g); y++;
        g.gridy = y; g.gridx = 0; p.add(new JLabel("Novo Nome (vazio p/ ignorar):"), g); g.gridx = 1; fNovoNome.setColumns(15); p.add(fNovoNome, g); y++;
        g.gridy = y; g.gridx = 0; p.add(new JLabel("Nova Senha (vazio p/ ignorar):"), g); g.gridx = 1; fNovaSenha.setColumns(15); p.add(fNovaSenha, g); y++;
        g.gridy = y; g.gridx = 0; g.gridwidth = 2; JButton bSalvar = new JButton("Salvar Alterações"); bSalvar.setBackground(new Color(40, 167, 69)); bSalvar.setForeground(Color.WHITE); bSalvar.addActionListener(e -> executarAtualizacao()); p.add(bSalvar, g); y++;
        JButton bDel = new JButton("Apagar Minha Conta Permanentemente"); bDel.setBackground(Color.RED); bDel.setForeground(Color.WHITE); g.gridy = y; p.add(bDel, g);
        bDel.addActionListener(e -> { int confirma = JOptionPane.showConfirmDialog(this, "Tem certeza?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION); if (confirma == JOptionPane.YES_OPTION) { MensagemDTO req = new MensagemDTO(); req.op = "deletarUsuario"; req.token = meuToken; enviarDadosAssincrono(req); fecharSessao(); } });
        JPanel container = new JPanel(new BorderLayout()); container.setBorder(new EmptyBorder(30, 30, 30, 30)); container.add(p, BorderLayout.NORTH); return container;
    }

    private void executarAtualizacao() {
        String novoNome = fNovoNome.getText().trim(); String novaSenha = new String(fNovaSenha.getPassword()).trim();
        if (novoNome.isEmpty() && novaSenha.isEmpty()) { JOptionPane.showMessageDialog(this, "Preencha pelo menos um campo para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
        MensagemDTO req = new MensagemDTO(); req.op = "atualizarUsuario"; req.token = fTokenUsuario.getText().trim(); req.nome = novoNome.isEmpty() ? null : novoNome; req.senha = novaSenha.isEmpty() ? null : novaSenha; 
        enviarDadosAssincrono(req); fNovoNome.setText(""); fNovaSenha.setText("");
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true)); }
}