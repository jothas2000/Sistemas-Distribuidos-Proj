import java.util.List;

public class MensagemDTO {
    // Adicionado token_admin
    public String op, usuario, nome, senha, novaSenha, token, token_admin, texto, mensagem, resposta;
    public List<MensagemDTO> historico; 
    
    // Lista específica para o retorno da consulta do Administrador
    public List<UsuarioDTO> lista_usuarios; 

    public MensagemDTO() {}
    
    // Construtor usado pelo Servidor para gravar o histórico
    public MensagemDTO(String usuario, String nome, String texto) {
        this.usuario = usuario;
        this.nome = nome;
        this.texto = texto;
    }

    // Classe aninhada estática (O Gson converte isso perfeitamente para JSON)
    public static class UsuarioDTO {
        public String nome;
        public String usuario;

        public UsuarioDTO(String nome, String usuario) {
            this.nome = nome;
            this.usuario = usuario;
        }
    }
}