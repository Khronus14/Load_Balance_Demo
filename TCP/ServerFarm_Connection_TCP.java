package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerFarm_Connection_TCP extends Thread {
    private final Socket socket;
    private final ServerThread_TCP server;

    public ServerFarm_Connection_TCP(Socket socket, ServerThread_TCP server) {
        super("BackConnection");
        this.socket = socket;
        this.server = server;
    }
    public void run() {
        this.server.updateConnections(1);

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(
                     socket.getInputStream()))) {

            String fromPortal;
            String toPortal = "";

            // read, parse, and respond to client messages
            while ((fromPortal = in.readLine()) != null) {
                toPortal = LB_Protocol.parseString(fromPortal);
                out.println(toPortal);
                // if counting complete, close connection
                if (toPortal.startsWith(LB_Protocol.COMPLETE)) break;
            }

            String[] finalMessage = toPortal.split(" ");
            this.server.updateUsage(Integer.parseInt(finalMessage[2]));
            socket.close();
        } catch (IOException e) {
            String err = "IOException with server-portal connection.";
            System.out.println(err);
        }

        this.server.updateConnections(-1);
    }
}
