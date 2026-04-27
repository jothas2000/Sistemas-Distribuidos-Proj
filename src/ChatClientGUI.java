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

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 

        painelPrincipal.add(criarTelaLogin(), "LOGIN");
        painelPrincipal.add(criarTelaApp(), "APP");
        add(painelPrincipal);
        
        conectar();
    }

    private void conectar() {
        try {
            socket = new Socket("127.0.0.1", 8080);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (Exception e) { System.out.println("Servidor Offline"); }
    }

    private JPanel criarTelaLogin() {
        JPanel painelFundo = new JPanel(new GridBagLayout());
        JPanel caixaLogin = new JPanel(new GridBagLayout());
        caixaLogin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(20, 30, 20, 30)));
        caixaLogin.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Acesso ao Sistema", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        caixaLogin.add(titulo, gbc);

        JTextField u = new JTextField(15); 
        JPasswordField s = new JPasswordField(15);
        
        gbc.gridy = 1; gbc.gridwidth = 1;
        caixaLogin.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; caixaLogin.add(u, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        caixaLogin.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; caixaLogin.add(s, gbc);

        JButton bL = new JButton("Entrar"); 
        bL.setBackground(new Color(70, 130, 180)); bL.setForeground(Color.WHITE);
        JButton bC = new JButton("Cadastrar");
        bC.setBackground(new Color(34, 139, 34)); bC.setForeground(Color.WHITE);

        JPanel pBotoes = new JPanel(new GridLayout(1, 2, 10, 0));
        pBotoes.setOpaque(false);
        pBotoes.add(bL); pBotoes.add(bC);
        
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        caixaLogin.add(pBotoes, gbc);
        painelFundo.add(caixaLogin);

        bL.addActionListener(e -> {
            MensagemDTO res = enviar(u.getText(), new String(s.getPassword()), "login", null);
            if (res != null) {
                if ("200".equals(res.resposta)) {
                    meuUsuario = u.getText(); meuToken = res.token;
                    configurarAbas();
                    cardLayout.show(painelPrincipal, "APP");
                    atualizarChat(); // Faz a leitura inicial ao entrar
                } else {
                    JOptionPane.showMessageDialog(this, res.mensagem, "Falha no Login", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        bC.addActionListener(e -> {
            MensagemDTO res = enviar(u.getText(), new String(s.getPassword()), "create", null);
            if (res != null) {
                if ("200".equals(res.resposta)) {
                    JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, res.mensagem, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        return painelFundo;
    }

    private void configurarAbas() {
        if ("adm".equals(meuToken)) {
            if (abasApp.indexOfTab("Controle Admin") == -1) abasApp.addTab("Controle Admin", painelAdmin);
        } else {
            int idx = abasApp.indexOfTab("Controle Admin");
            if (idx != -1) abasApp.remove(idx);
        }
    }

    private Container criarTelaApp() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(550);
        
        JPanel pChat = new JPanel(new BorderLayout());
        areaChatPane.setEditable(false);
        
        JTextField tMsg = new JTextField(); 
        JButton bEnv = new JButton("Enviar"); 
        JButton bAtu = new JButton("Atualizar Mensagens"); // Botão manual restaurado
        
        JPanel pS = new JPanel(new BorderLayout());
        pS.add(tMsg, BorderLayout.CENTER); pS.add(bEnv, BorderLayout.EAST); pS.add(bAtu, BorderLayout.NORTH);
        pChat.add(new JScrollPane(areaChatPane), BorderLayout.CENTER); pChat.add(pS, BorderLayout.SOUTH);
        abasApp.addTab("Chat Geral", pChat);

        painelAdmin = new JPanel(new FlowLayout());
        JTextField tDel = new JTextField(15); 
        JButton bDel = new JButton("Excluir Usuário");
        painelAdmin.add(new JLabel("Nome de usuário:")); painelAdmin.add(tDel); painelAdmin.add(bDel);
        bDel.addActionListener(e -> {
            MensagemDTO res = enviar(tDel.getText(), null, "delete", null);
            if(res != null) {
                if("200".equals(res.resposta)) JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                else JOptionPane.showMessageDialog(this, res.mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel pL = new JPanel(new BorderLayout());
        areaLogs.setBackground(Color.BLACK); areaLogs.setForeground(Color.GREEN);
        areaLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JButton bOut = new JButton("Logout"), bBye = new JButton("Sair (Bye)");
        JPanel pB = new JPanel(new GridLayout(2,1)); pB.add(bOut); pB.add(bBye);
        pL.add(new JScrollPane(areaLogs), BorderLayout.CENTER); pL.add(pB, BorderLayout.SOUTH);

        split.setLeftComponent(abasApp); split.setRightComponent(pL);

        bEnv.addActionListener(e -> { 
            enviar(meuUsuario, null, "send", tMsg.getText()); 
            tMsg.setText(""); 
            atualizarChat(); 
        });
        
        // Ação manual do botão
        bAtu.addActionListener(e -> atualizarChat());
        
        bOut.addActionListener(e -> {
            enviar(meuUsuario, null, "bye", null);
            try { socket.close(); } catch (Exception ex) {}
            conectar(); 
            cardLayout.show(painelPrincipal, "LOGIN");
        });
        
        bBye.addActionListener(e -> { 
            enviar(meuUsuario, null, "bye", null); 
            System.exit(0); 
        });

        return split;
    }

    private void atualizarChat() {
        MensagemDTO res = enviar(meuUsuario, null, "read", null);
        if (res != null && res.historico != null) {
            areaChatPane.setText(""); 
            for (MensagemDTO m : res.historico) {
                if ("Sistema-Enter".equals(m.usuario)) {
                    adicionarTextoColorido(m.texto + "\n", new Color(0, 128, 0)); 
                } else if ("Sistema-Delete".equals(m.usuario)) {
                    adicionarTextoColorido(m.texto + "\n", Color.RED); 
                } else {
                    adicionarTextoColorido("["+m.usuario+"]: " + m.texto + "\n", Color.BLACK);
                }
            }
        }
    }

    private void adicionarTextoColorido(String texto, Color cor) {
        StyledDocument doc = areaChatPane.getStyledDocument();
        Style estilo = areaChatPane.addStyle("Estilo", null);
        StyleConstants.setForeground(estilo, cor);
        try { doc.insertString(doc.getLength(), texto, estilo); } 
        catch (BadLocationException e) { e.printStackTrace(); }
    }

    private MensagemDTO enviar(String u, String s, String op, String t) {
        try {
            if (socket == null || socket.isClosed()) conectar();
            MensagemDTO req = new MensagemDTO(); req.op=op; req.usuario=u; req.senha=s; req.texto=t; req.token=meuToken;
            String j = gson.toJson(req); out.println(j);
            
            // Reativei o log para a operação read, assim você vê exatamente o que trafega
            areaLogs.append("-> "+j+"\n"); areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
            
            String r = in.readLine(); 
            areaLogs.append("<- "+r+"\n"); areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
            
            return gson.fromJson(r, MensagemDTO.class);
        } catch (Exception e) { return null; }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true)); }
}