import java.util.List;

public class MensagemDTO {
    public String op, usuario, senha, novaSenha, token, texto, mensagem, resposta;
    public List<MensagemDTO> historico; 

    public MensagemDTO() {}
    
    public MensagemDTO(String usuario, String texto) {
        this.usuario = usuario;
        this.texto = texto;
    }
}