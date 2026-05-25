import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    public ChatClientGUI() {
        setTitle("Chat Distribuído - UTFPR (EP-2)");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 

        painelPrincipal.add(criarTelaLogin(), "LOGIN");
        painelPrincipal.add(criarTelaApp(), "APP");
        add(painelPrincipal);
    }
    // classe que abre a conexão do cliente
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
        
        // botão de login pega o usuario e senha escritos no input e faz a verificação se um dos dois está vazio e depois verifica se a função conectar retorna true com o ip e a porta enviada corretos, e então verifica se a resposta do servidor é diferente de NULL e a resposta da 200, e então pega o usuário e o token do usuário, e inicializa o campo de teste com o token obtido no login, e ao realizar o login consulta o usuário e se der ok coloca o nome do usuario, se não a o login não é realizado e alegue uma mensagem de erro e lança um logout fechando o socket 
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
                    fTokenUsuario.setText(meuToken); // Inicializa o campo de teste com o token real obtido

                    MensagemDTO resNome = enviarDados(u, null, null, "consultarUsuario", null);
                    if (resNome != null && "200".equals(resNome.resposta)) {
                        meuUsuario = resNome.nome; 
                                        
                        if (resNome.token != null) {
                            meuToken = resNome.token; 
                            fTokenUsuario.setText(meuToken);
                        }
                    }
                    configurarAbas(); 
                    cardLayout.show(painelPrincipal, "APP");
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de conexão", "Falha", JOptionPane.ERROR_MESSAGE);
                    enviarDados(u, null, null, "logout", null);
                    try { socket.close(); } catch (Exception ex) {}
                }
            }
        });
        
        // botão de cadastro pega o nome o usuario e a senha, verifica se um deles está vazio, se tiver retorna erro e faz a mesma verificação antes de conectar do ip e porta, e manda a requisição de cadastrar o usuário, e caso a requisição esteja vazia sinaliza erro e um aviso vindo do servidor, e ai fecha o socket para que possa receber outra conexão
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
                try { socket.close(); } catch (Exception ex) {}
            }
        });

        return painelFundo;
    }
    // configuração da aba, caso o token seja adm, vai para a aba do adm, se não vai a página do usuário comum, fazendo removendo a aba do adm 
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
    // criação da tela bgrbgrbgbrbgr
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
        pAcoes.add(bAtu); pAcoes.add(bDel); 

        JPanel pSul = new JPanel(new BorderLayout());
        pSul.add(pEnvio, BorderLayout.NORTH);
        pSul.add(pAcoes, BorderLayout.SOUTH);

        pChat.add(new JScrollPane(areaChatPane), BorderLayout.CENTER); 
        pChat.add(pSul, BorderLayout.SOUTH);
        
        abasApp.addTab("Chat Geral", pChat);
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

        split.setLeftComponent(abasApp); split.setRightComponent(pLogs);

        bEnv.addActionListener(e -> { 
            if(!tMsg.getText().trim().isEmpty()) {
                enviarDados(meuUsuario, null, null, "enviarMensagem", tMsg.getText()); 
                tMsg.setText(""); 
            }
        });
        
        // botão de delete, começa um modal que pergunta se o usuário quer apagar a conta, e tem uma variavel confirma, se a opção for igual a sim, chama a resposta de deletarUsuario, e se der tudo certo, manda sucesso fecha o socket e volta o layout para o login, se não da erro ao deletar e volta uma mensagem
        bDel.addActionListener(e -> {
            int confirma = JOptionPane.showConfirmDialog(this, "Tem certeza que deseja apagar sua conta permanentemente?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
            
            if (confirma == JOptionPane.YES_OPTION) {
                MensagemDTO res = enviarDados(null, null, null, "deletarUsuario", null);
                if (res != null && "200".equals(res.resposta)) {
                    JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    try { socket.close(); } catch (Exception ex) {}
                    cardLayout.show(painelPrincipal, "LOGIN");
                } else {
                    JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro ao deletar", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // botão de logout envia a requisição de logout, fecha o socket e apaga tudo do Log e do painel de chat, e depois troca o layout para o login
        bOut.addActionListener(e -> {
            enviarDados(meuUsuario, null, null, "logout", meuToken);
            try { socket.close(); } catch (Exception ex) {}
            areaLogs.setText(""); areaChatPane.setText("");
            cardLayout.show(painelPrincipal, "LOGIN");
        });
        
        // da logout e fecha o Cliente
        bBye.addActionListener(e -> { 
            enviarDados(meuUsuario, null, null, "logout", meuToken); 
            System.exit(0); 
        });

        return split;
    }

    // cria o painel de Cliente Admin
    private JPanel criarPainelAdmin() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8); 
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

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

        // botão de listar os usuários na tela do ADM, utiliza o token do adm para consultar os usuários, processa o objeto e verifica se a lista não está vázia eou a resposta é diferente de null ou a resposta é 200, builda a
        bListar.addActionListener(e -> {
            // Usa o token do campo de teste do admin
            MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuariosAdmin"; m.token_admin = fTokenAdmin.getText().trim();
            MensagemDTO res = processarObjeto(m);
            if (res != null && "200".equals(res.resposta) && res.lista_usuarios != null) {
                StringBuilder sb = new StringBuilder("=== USUÁRIOS NO SISTEMA ===\n\n");
                for(MensagemDTO.UsuarioDTO user : res.lista_usuarios) {
                    sb.append("Login: ").append(user.usuario).append("  |  Nome: ").append(user.nome).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString(), "Lista de Usuários", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro de conexão", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        // Botão de consulta do ADMIN faz a consulta dos usuarios pelo adm e retorna os nome de exibição
        bCons.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "consultarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fConsUser.getText().trim();
            MensagemDTO res = processarObjeto(m);
            if (res != null && "200".equals(res.resposta)) {
                JOptionPane.showMessageDialog(this, "Login: " + res.usuario + "\nNome Exibição: " + res.nome, "Dados Encontrados", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        // Botão de atualização do usuário pelo ADMIN, processa os objetos pegados pela interface, podendo ter modificações parciais, se a resposta for 200, da certo e atualiza, mandando uma mensagem de sucesso
        bAtu.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "atualizarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fAtuUser.getText().trim();
            // Se os campos de alteração estiverem vazios, passa null de acordo com o protocolo de modificação parcial
            m.nome = fAtuNome.getText().trim().isEmpty() ? null : fAtuNome.getText().trim(); 
            m.senha = fAtuSenha.getText().trim().isEmpty() ? null : fAtuSenha.getText().trim();
            MensagemDTO res = processarObjeto(m);
            if (res != null && "200".equals(res.resposta)) JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            else JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Erro", JOptionPane.ERROR_MESSAGE);
        });

        // Botão delete do ADMIN, o admin tem q escrever o usuário a ser deletado e é enviado do input para um objeto m, que tem o token do Admin, e o usuário a ser deletado, criando o DTO, e processando o objeto, onde é feita a identificação da requisição e de fato é deletado
        bDel.addActionListener(e -> {
            MensagemDTO m = new MensagemDTO(); m.op = "deletarUsuarioAdmin"; 
            m.token_admin = fTokenAdmin.getText().trim(); m.usuario = fDelUser.getText().trim();
            MensagemDTO res = processarObjeto(m);
            if (res != null && "200".equals(res.resposta)) JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            else JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro", "Erro", JOptionPane.ERROR_MESSAGE);
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
        JLabel titulo = new JLabel("Atualizar Cadastro", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(titulo, g);
        y++;

        g.gridwidth = 1; 
        
        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Modificar Token Usuário (Teste):"), g);
        g.gridx = 1; p.add(fTokenUsuario, g);
        y++;

        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Novo Nome (deixe em branco p/ manter):"), g);
        g.gridx = 1; fNovoNome.setColumns(15); p.add(fNovoNome, g);
        y++;

        g.gridy = y; g.gridx = 0;
        p.add(new JLabel("Nova Senha (deixe em branco p/ manter):"), g);
        g.gridx = 1; fNovaSenha.setColumns(15); p.add(fNovaSenha, g);
        y++;

        g.gridy = y; g.gridx = 0; g.gridwidth = 2;
        JButton bSalvar = new JButton("Salvar Alterações");
        bSalvar.setBackground(new Color(40, 167, 69)); bSalvar.setForeground(Color.WHITE);
        bSalvar.addActionListener(e -> executarAtualizacao());
        p.add(bSalvar, g);

        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(new EmptyBorder(30, 30, 30, 30));
        container.add(p, BorderLayout.NORTH);
        return container;
    }
    // Executa a atualização do painel ao clickar no botão
    private void executarAtualizacao() {
        String novoNome = fNovoNome.getText().trim();
        String novaSenha = new String(fNovaSenha.getPassword()).trim();

        if (novoNome.isEmpty() && novaSenha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha pelo menos um campo para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MensagemDTO req = new MensagemDTO();
        req.op = "atualizarUsuario";
        // Envia o token que está no campo editável da tela para validação cruzada do professor
        req.token = fTokenUsuario.getText().trim(); 
        req.nome = novoNome.isEmpty() ? null : novoNome;   
        req.senha = novaSenha.isEmpty() ? null : novaSenha; 

        MensagemDTO res = processarObjeto(req);

        if (res != null && "200".equals(res.resposta)) {
            JOptionPane.showMessageDialog(this, res.mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            fNovoNome.setText("");
            fNovaSenha.setText("");
            if (!novoNome.isEmpty()) meuUsuario = novoNome; 
        } else {
            JOptionPane.showMessageDialog(this, res != null ? res.mensagem : "Erro no servidor", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    // cria o objeto a ser enviado ao servidor
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
            
            if ("logout".equalsIgnoreCase(req.op)) return null; 

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