package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Portal_Connection_TCP extends Thread {
    private final Socket frontSocket;
    private final String serverAddr;
    private final int serverPort;
    private final int server;
    private final Portal_TCP portal;

    public Portal_Connection_TCP(Socket socket, String serverAddr, int serverPort,
                                 int server, Portal_TCP portal) {
        super("PortalConnection");
        this.frontSocket = socket;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.server = server;
        this.portal = portal;
    }

    /**
     * Portal logic.
     */
    public void run() {
        try (PrintWriter frontOut = new PrintWriter(frontSocket.getOutputStream(), true);
             BufferedReader frontIn = new BufferedReader(new InputStreamReader(frontSocket.getInputStream()))) {

            Socket backSocket = new Socket(this.serverAddr, this.serverPort);
            PrintWriter backOut = new PrintWriter(backSocket.getOutputStream(), true);
            BufferedReader backIn = new BufferedReader(new InputStreamReader(backSocket.getInputStream()));

            String fromClient, fromServer;

            // read, parse, and respond to client messages
            while ((fromClient = frontIn.readLine()) != null) {
                // pass client request to server and wait for response
                backOut.println(fromClient);
                fromServer = backIn.readLine();

                // pass server response back to client
                frontOut.println(fromServer);
                // if counting complete, close connection
                if (fromServer.startsWith(LB_Protocol.COMPLETE)) break;
            }

            frontSocket.close();
            backSocket.close();
        } catch (IOException e) {
            String err = "IOException with client-portal connection.";
            System.err.println(err);
//            System.out.println("addr: " + this.serverAddr + " port: " + this.serverPort);
        }
    }
}
