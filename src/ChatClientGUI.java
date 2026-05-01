import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

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

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR (EP-1)");
        setSize(950, 600);
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

        // --- AÇÃO DO BOTÃO LOGIN ---
        bLogin.addActionListener(e -> {
            String u = fUser.getText().trim();
            String s = new String(fPass.getPassword()).trim();
            
            if(u.isEmpty() || s.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Usuário e senha vazios!", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO res = enviarDados(u, null, s, "login", null);
                if (res != null && "200".equals(res.resposta)) {
                    meuUsuario = u; meuToken = res.token;

                    MensagemDTO resNome = enviarDados(u, null, null, "consultarUsuario", null);
                    if (resNome != null && "200".equals(resNome.resposta)) {
                        meuUsuario = resNome.nome; // Agora a variável guarda o Nome Real e não apenas o login
                                        
                        // AQUI: Reafirma o token recebido pela consulta
                        if (resNome.token != null) {
                            meuToken = resNome.token; 
                        }
                    }
                    configurarAbas(); 
                    cardLayout.show(painelPrincipal, "APP");
                    //atualizarChat(); 
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de conexão", "Falha", JOptionPane.ERROR_MESSAGE);
                    // O PULO DO GATO: Se o login falhou, libere o servidor!
                    enviarDados(u, null, null, "logout", null);
                    try { socket.close(); } catch (Exception ex) {}
                }
            }
        });
        
        // --- AÇÃO DO BOTÃO CADASTRAR ---
        bCad.addActionListener(e -> {
            String n = fNome.getText().trim();
            String u = fUser.getText().trim();
            String s = new String(fPass.getPassword()).trim();
            
            if(n.isEmpty() || u.isEmpty() || s.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha Nome, Usuário e Senha para cadastrar!", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (conectar(fIP.getText().trim(), Integer.parseInt(fPorta.getText().trim()))) {
                MensagemDTO res = enviarDados(u, n, s, "cadastrarUsuario", null);
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                
                // O PULO DO GATO: Após tentar cadastrar, libere o servidor para a próxima ação!
                enviarDados(u, null, null, "logout", null); 
                try { socket.close(); } catch (Exception ex) {}
            }
        });

        return painelFundo;
    }

    private void configurarAbas() {
        // A interface básica já é carregada no criarTelaApp.
        // Aqui apenas controlamos se a aba "Admin" deve aparecer ou não.
        if ("adm".equals(meuToken)) {
            if (abasApp.indexOfTab("Admin") == -1) {
                abasApp.addTab("Admin", painelAdmin);
            }
        } else {
            int index = abasApp.indexOfTab("Admin");
            if (index != -1) abasApp.removeTabAt(index);
        }
    }

    private Container criarTelaApp() {

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(550);
        
        JPanel pChat = new JPanel(new BorderLayout());
        areaChatPane.setEditable(false);
        areaChatPane.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JTextField tMsg = new JTextField(); 
        JButton bEnv = new JButton("Enviar Msg"); 
        
        JPanel pEnvio = new JPanel(new BorderLayout(5, 5));
        pEnvio.setBorder(new EmptyBorder(5, 5, 5, 5));
        pEnvio.add(tMsg, BorderLayout.CENTER); pEnvio.add(bEnv, BorderLayout.EAST);
        
        JPanel pAcoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton bAtu = new JButton("Atualizar Histórico"); 
        JButton bDel = new JButton("Apagar Minha Conta");
        pAcoes.add(bAtu); pAcoes.add(bDel); // Botão antigo de alterar senha foi removido daqui

        JPanel pSul = new JPanel(new BorderLayout());
        pSul.add(pEnvio, BorderLayout.NORTH);
        pSul.add(pAcoes, BorderLayout.SOUTH);

        pChat.add(new JScrollPane(areaChatPane), BorderLayout.CENTER); 
        pChat.add(pSul, BorderLayout.SOUTH);
        
        abasApp.addTab("Chat Geral", pChat);
        abasApp.addTab("Perfil/Config", criarPainelConfiguracoes()); // AQUI A NOVA ABA É ADICIONADA

        painelAdmin = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        JTextField tDelAdmin = new JTextField(15); 
        JButton bDelAdmin = new JButton("Excluir Usuário");
        painelAdmin.add(new JLabel("Usuário alvo:")); 
        painelAdmin.add(tDelAdmin); 
        painelAdmin.add(bDelAdmin);
        
        bDelAdmin.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "deletarUsuario"; m.usuario = tDelAdmin.getText(); m.token = meuToken;
            MensagemDTO res = processarObjeto(m);
            if(res != null) {
                if("200".equals(res.resposta)) JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                else JOptionPane.showMessageDialog(this, res.mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel pLogs = new JPanel(new BorderLayout());
        areaLogs.setBackground(Color.BLACK); areaLogs.setForeground(Color.GREEN);
        areaLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLogs.setEditable(false);
        
        JButton bOut = new JButton("Logout (Voltar)"), bBye = new JButton("Sair do App");
        JPanel pBotoesSair = new JPanel(new GridLayout(2,1)); 
        pBotoesSair.add(bOut); pBotoesSair.add(bBye);
        
        pLogs.add(new JScrollPane(areaLogs), BorderLayout.CENTER); 
        pLogs.add(pBotoesSair, BorderLayout.SOUTH);

        split.setLeftComponent(abasApp); split.setRightComponent(pLogs);

        bEnv.addActionListener(e -> { 
            if(!tMsg.getText().trim().isEmpty()) {
                enviarDados(meuUsuario, null, null, "enviarMensagem", tMsg.getText()); 
                tMsg.setText(""); 
                //atualizarChat(); 
            }
        });
        
        //bAtu.addActionListener(e -> atualizarChat());
        


        bDel.addActionListener(e -> {
            int confirma = JOptionPane.showConfirmDialog(this, "Tem certeza que deseja apagar sua conta permanentemente?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
            
            if (confirma == JOptionPane.YES_OPTION) {
                // Chamada limpa: passamos null para usuário, nome, senha e texto.
                // O método enviarDados anexará o meuToken automaticamente.
                // Gerando o JSON: {"op": "deletarUsuario", "token": "..."}
                MensagemDTO res = enviarDados(null, null, null, "deletarUsuario", null);

                if (res != null && "200".equals(res.resposta)) {
                    JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    // Como a conta não existe mais, forçamos o logout e voltamos para a tela de login
                    try { socket.close(); } catch (Exception ex) {}
                    cardLayout.show(painelPrincipal, "LOGIN");
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro ao deletar", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        bOut.addActionListener(e -> {
            enviarDados(meuUsuario, null, null, "logout", meuToken);
            try { socket.close(); } catch (Exception ex) {}
            areaLogs.setText(""); areaChatPane.setText("");
            cardLayout.show(painelPrincipal, "LOGIN");
        });
        
        bBye.addActionListener(e -> { 
            enviarDados(meuUsuario, null, null, "logout", meuToken); 
            System.exit(0); 
        });

        return split;
    }

    /* private void atualizarChat() {
        MensagemDTO res = enviarDados(meuUsuario, null, null, "lerMensagens", null);
        if (res != null && res.historico != null) {
            areaChatPane.setText(""); 
            for (MensagemDTO m : res.historico) {
                // A REGRA DAS CORES PEDIDA:
                if ("Sistema-Enter".equals(m.usuario)) {
                    adicionarTextoColorido(m.texto + "\n", new Color(0, 150, 0)); // Verde
                } else if ("Sistema-Delete".equals(m.usuario)) {
                    adicionarTextoColorido(m.texto + "\n", Color.RED); // Vermelho
                } else {
                    // Mensagens normais ficam Pretas e exibem o NOME
                    String nomeExibicao = (m.nome != null) ? m.nome : m.usuario;
                    adicionarTextoColorido("[" + nomeExibicao + "]: " + m.texto + "\n", Color.BLACK); 
                }
            }
        }
    }
 */
 /*    private void adicionarTextoColorido(String texto, Color cor) {
        StyledDocument doc = areaChatPane.getStyledDocument();
        Style estilo = areaChatPane.addStyle("Estilo", null);
        StyleConstants.setForeground(estilo, cor);
        StyleConstants.setBold(estilo, true);
        try { doc.insertString(doc.getLength(), texto, estilo); } 
        catch (BadLocationException e) { e.printStackTrace(); }
    } */

    private JPanel criarPainelConfiguracoes() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        JLabel titulo = new JLabel("Atualizar Cadastro", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(titulo, g);

        g.gridwidth = 1; g.gridy = 1; g.gridx = 0;
        p.add(new JLabel("Novo Nome (deixe em branco p/ manter):"), g);
        g.gridx = 1; fNovoNome.setColumns(15); p.add(fNovoNome, g);

        g.gridy = 2; g.gridx = 0;
        p.add(new JLabel("Nova Senha (deixe em branco p/ manter):"), g);
        g.gridx = 1; fNovaSenha.setColumns(15); p.add(fNovaSenha, g);

        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        JButton bSalvar = new JButton("Salvar Alterações");
        bSalvar.setBackground(new Color(40, 167, 69)); bSalvar.setForeground(Color.WHITE);
        bSalvar.addActionListener(e -> executarAtualizacao());
        p.add(bSalvar, g);

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

        // Monta o JSON perfeitamente alinhado com o payload exigido (incluindo as strings vazias)
        MensagemDTO req = new MensagemDTO();
        req.op = "atualizarUsuario";
        req.token = meuToken;
        req.nome = novoNome;   // Ficará "" se não preenchido
        req.senha = novaSenha; // Ficará "" se não preenchido

        MensagemDTO res = processarObjeto(req);

        if (res != null && "200".equals(res.resposta)) {
            JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            fNovoNome.setText("");
            fNovaSenha.setText("");
            if (!novoNome.isEmpty()) meuUsuario = novoNome; // Atualiza localmente
        } else {
            JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro no servidor", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private MensagemDTO enviarDados(String u, String n, String s, String op, String t) {
        MensagemDTO req = new MensagemDTO(); req.op=op; req.usuario=u; req.nome=n; req.senha=s; req.texto=t; req.token=meuToken;
        return processarObjeto(req);
    }

    private MensagemDTO processarObjeto(MensagemDTO req) {
        try {
            String jsonRequest = gson.toJson(req); 
            out.println(jsonRequest);
            
            areaLogs.append("-> " + jsonRequest + "\n"); 
            areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
            
            if ("logout".equalsIgnoreCase(req.op)) return null; // Não espera resposta no logout final

            String jsonResponse = in.readLine(); 
            areaLogs.append("<- " + jsonResponse + "\n\n"); 
            areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
            
            return gson.fromJson(jsonResponse, MensagemDTO.class);
        } catch (Exception e) { 
            return null; 
        }
    }

    public static void main(String[] args) { 
        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true)); 
    }
}
