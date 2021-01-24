package client;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientWriteThread extends Thread {
    private PrintWriter writer;
    private boolean connected;
    
    public ClientWriteThread(Socket socket, GameClient client) {
        try {
            writer = new PrintWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(), "UTF8"), 
                        true);
            connected = true;
        } catch (IOException e) {
            client.getKicked();
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        while (connected) {}
    }
    
    public void write(String s) {
        writer.println(s);
    }
    
    public void disconnect() {
        connected = false;
    }
}