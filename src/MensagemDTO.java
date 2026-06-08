import java.util.List;
import java.util.Map;

public class MensagemDTO {
    public String op, usuario, nome, senha, token, token_admin, mensagem, resposta;
    
    // Novos campos para o EP-3
    public String destinatario; 
    public String remetente;
    public List<String> usuarios; // Usado no ListarUsuariosLogados

    // Mantido para o Admin
    public List<Map<String, String>> lista_usuarios; 

    public MensagemDTO() {}
}