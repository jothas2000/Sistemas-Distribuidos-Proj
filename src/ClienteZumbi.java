import java.io.PrintWriter;
import java.net.Socket;

public class ClienteZumbi {
    public static void main(String[] args) throws Exception {
        System.out.println("ZUMBI: Conectando ao servidor...");
        Socket s = new Socket("127.0.0.1", 8080);
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        
        // Manda o logout e NÃO FECHA o socket!
        System.out.println("ZUMBI: Mandando logout...");
        out.println("{\"op\":\"logout\", \"token\":\"qualquer\"}");
        
        System.out.println("ZUMBI: Ficarei dormindo por 1 minuto prendendo a linha...");
        Thread.sleep(60000); // Dorme por 60 segundos
        System.out.println("ZUMBI: Morri.");
		s.close();
    }
}