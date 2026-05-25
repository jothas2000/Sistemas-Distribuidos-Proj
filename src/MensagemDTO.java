import java.util.List;
import java.util.Map;

public class MensagemDTO {
    public String op, usuario, nome, senha, novaSenha, token, token_admin, texto, mensagem, resposta;
    public List<MensagemDTO> historico; 
    
    // Agora é uma lista de dicionários (Maps) para suportar as chaves dinâmicas (usuario1, nome2, etc)
    public List<Map<String, String>> lista_usuarios; 

    public MensagemDTO() {}
    
    public MensagemDTO(String usuario, String nome, String texto) {
        this.usuario = usuario;
        this.nome = nome;
        this.texto = texto;
    }
}