import java.util.List;

public class MensagemDTO {
    public String op, usuario, nome, senha, novaSenha, token, token_admin, texto, mensagem, resposta;
    public List<MensagemDTO> historico; 
    
    // lista para a consulta do adm
    public List<UsuarioDTO> lista_usuarios; 

    public MensagemDTO() {}
    
    public MensagemDTO(String usuario, String nome, String texto) {
        this.usuario = usuario;
        this.nome = nome;
        this.texto = texto;
    }

    // construtor do gson
    public static class UsuarioDTO {
        public String nome;
        public String usuario;

        public UsuarioDTO(String nome, String usuario) {
            this.nome = nome;
            this.usuario = usuario;
        }
    }
}