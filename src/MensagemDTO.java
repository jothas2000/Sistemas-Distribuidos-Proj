import java.util.List;

public class MensagemDTO {
    public String op, usuario, nome, senha, novaSenha, token, texto, mensagem, resposta;
    public List<MensagemDTO> historico; 

    public MensagemDTO() {}
    
    // Construtor usado pelo Servidor para gravar o histórico
    public MensagemDTO(String usuario, String nome, String texto) {
        this.usuario = usuario;
        this.nome = nome;
        this.texto = texto;
    }
}