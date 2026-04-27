import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JTextArea areaChat = new JTextArea();

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR");
        setSize(800, 500);
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
            JOptionPane.showMessageDialog(this, "Erro ao conectar: Verifique IP, Porta e se o Servidor está online.", "Falha de Conexão", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JPanel criarTelaLogin() {
        JPanel painelFundo = new JPanel(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), new EmptyBorder(20, 30, 20, 30)));
        p.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8,8,8,8); g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Acesso ao Chat", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        g.gridx=0; g.gridy=0; g.gridwidth=2; p.add(titulo, g);

        g.gridwidth=1;
        JTextField fIP = new JTextField("127.0.0.1", 12);
        JTextField fPorta = new JTextField("8080", 12);
        JTextField fUser = new JTextField(12);
        JPasswordField fPass = new JPasswordField(12);

        g.gridx=0; g.gridy=1; p.add(new JLabel("IP Servidor:"), g);
        g.gridx=1; p.add(fIP, g);
        g.gridx=0; g.gridy=2; p.add(new JLabel("Porta:"), g);
        g.gridx=1; p.add(fPorta, g);
        
        g.gridx=0; g.gridy=3; p.add(new JLabel("Usuário:"), g);
        g.gridx=1; p.add(fUser, g);
        g.gridx=0; g.gridy=4; p.add(new JLabel("Senha:"), g);
        g.gridx=1; p.add(fPass, g);

        JButton bLogin = new JButton("Login");
        JButton bCad = new JButton("Cadastrar");
        
        JPanel pBotoes = new JPanel(new GridLayout(1, 2, 10, 0));
        pBotoes.setOpaque(false); pBotoes.add(bLogin); pBotoes.add(bCad);
        
        g.gridx=0; g.gridy=5; g.gridwidth=2; p.add(pBotoes, g);
        painelFundo.add(p);

        bLogin.addActionListener(e -> {
            if (conectar(fIP.getText(), Integer.parseInt(fPorta.getText()))) {
                MensagemDTO res = enviarDadosBasicos(fUser.getText(), new String(fPass.getPassword()), "login", null);
                if (res != null && "200".equals(res.resposta)) {
                    meuUsuario = fUser.getText(); meuToken = res.token;
                    cardLayout.show(painelPrincipal, "APP");
                    atualizarChat(); 
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        bCad.addActionListener(e -> {
            if (conectar(fIP.getText(), Integer.parseInt(fPorta.getText()))) {
                MensagemDTO res = enviarDadosBasicos(fUser.getText(), new String(fPass.getPassword()), "create", null);
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de comunicação.");
                // Encerra conexão do cadastro para não travar o servidor iterativo
                enviarDadosBasicos(meuUsuario, null, "bye", null);
            }
        });

        return painelFundo;
    }

    private Container criarTelaApp() {
        JPanel p = new JPanel(new BorderLayout());
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Monospaced", Font.PLAIN, 14));
        p.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        JPanel pSul = new JPanel(new BorderLayout(5, 5));
        pSul.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField tMsg = new JTextField();
        JButton bEnv = new JButton("Enviar Msg");
        JPanel pEnv = new JPanel(new BorderLayout(5, 0));
        pEnv.add(tMsg, BorderLayout.CENTER); pEnv.add(bEnv, BorderLayout.EAST);
        
        JPanel pAcoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton bAtu = new JButton("Atualizar Histórico");
        JButton bUpd = new JButton("Alterar Senha");
        JButton bDel = new JButton("Apagar Minha Conta");
        JButton bOut = new JButton("Logout / Sair");
        
        pAcoes.add(bAtu); pAcoes.add(bUpd); pAcoes.add(bDel); pAcoes.add(bOut);
        
        pSul.add(pEnv, BorderLayout.NORTH);
        pSul.add(pAcoes, BorderLayout.SOUTH);
        p.add(pSul, BorderLayout.SOUTH);

        bEnv.addActionListener(e -> { 
            if(!tMsg.getText().isEmpty()) {
                enviarDadosBasicos(meuUsuario, null, "send", tMsg.getText()); 
                tMsg.setText(""); 
                atualizarChat(); 
            }
        });
        
        bAtu.addActionListener(e -> atualizarChat());
        
        bOut.addActionListener(e -> { 
            enviarDadosBasicos(meuUsuario, null, "bye", null); 
            cardLayout.show(painelPrincipal, "LOGIN"); 
            try { socket.close(); } catch(Exception ex) {}
        });
        
        bUpd.addActionListener(e -> { 
            String nova = JOptionPane.showInputDialog(this, "Digite sua nova senha:");
            if (nova != null && !nova.trim().isEmpty()) {
                MensagemDTO m = new MensagemDTO(); m.op="update"; m.usuario=meuUsuario; m.token=meuToken; m.novaSenha=nova;
                MensagemDTO res = enviarObjeto(m);
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro");
            }
        });

        bDel.addActionListener(e -> { 
            int opc = JOptionPane.showConfirmDialog(this, "ATENÇÃO: Deseja realmente apagar sua conta?", "Exclusão", JOptionPane.YES_NO_OPTION);
            if (opc == JOptionPane.YES_OPTION) {
                MensagemDTO res = enviarDadosBasicos(meuUsuario, null, "delete", null);
                if (res != null && "200".equals(res.resposta)) {
                    JOptionPane.showMessageDialog(this, "Conta apagada.");
                    enviarDadosBasicos(meuUsuario, null, "bye", null);
                    cardLayout.show(painelPrincipal, "LOGIN");
                } else {
                    JOptionPane.showMessageDialog(this, "Erro ao apagar conta.");
                }
            }
        });

        return p;
    }

    private void atualizarChat() {
        MensagemDTO res = enviarDadosBasicos(meuUsuario, null, "read", null);
        if (res != null && res.historico != null) {
            areaChat.setText("");
            for (MensagemDTO m : res.historico) {
                areaChat.append(String.format("[%s]: %s\n", m.usuario, m.texto));
            }
        }
    }

    private MensagemDTO enviarDadosBasicos(String u, String s, String op, String t) {
        MensagemDTO m = new MensagemDTO(); m.op=op; m.usuario=u; m.senha=s; m.texto=t; m.token=meuToken;
        return enviarObjeto(m);
    }

    private MensagemDTO enviarObjeto(MensagemDTO m) {
        try {
            String jsonRequest = gson.toJson(m); 
            out.println(jsonRequest);
            System.out.println("-> CLIENTE ENVIOU: " + jsonRequest);
            
            // Se a operação for 'bye', não esperamos resposta do servidor
            if ("bye".equalsIgnoreCase(m.op)) return null;

            String jsonResponse = in.readLine(); 
            System.out.println("<- CLIENTE RECEBEU: " + jsonResponse);
            return gson.fromJson(jsonResponse, MensagemDTO.class);
        } catch (Exception e) { 
            return null; 
        }
    }

    public static void main(String[] args) { 
        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true)); 
    }
}