import java.util.List;

public class MensagemDTO {
    public String op, usuario, nome, senha, token, token_admin, mensagem, resposta;
    public String destinatario, remetente;
    
    public List<String> usuarios; // Usado pelo nosso servidor original
    public List<Object> lista_usuarios; // Usado pelo servidor externo (Aceita Strings ou Mapas)

    public MensagemDTO() {}
}