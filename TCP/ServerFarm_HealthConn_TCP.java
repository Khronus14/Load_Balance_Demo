package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerFarm_HealthConn_TCP extends Thread {
    private final Socket socket;
    private final ServerThread_TCP server;

    public ServerFarm_HealthConn_TCP(Socket socket, ServerThread_TCP server) {
        super("ServerHealth");
        this.socket = socket;
        this.server = server;
    }
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(
                     socket.getInputStream()))) {

            String fromPortal, toPortal;

            // read, parse, and respond to client messages
            while ((fromPortal = in.readLine()) != null) {
                if (fromPortal.startsWith(LB_Protocol.HEALTH)) {
                    String[] status = fromPortal.split(" ");
                    toPortal = String.format("%s %s %d %d",
                            LB_Protocol.HEALTH, status[1], this.server.connections, this.server.usage);
                    out.println(toPortal);
                    // reset usage
                    this.server.updateUsage(-this.server.usage);
                }

                // if counting complete, close connection
                if (fromPortal.startsWith(LB_Protocol.COMPLETE)) break;
            }

            socket.close();
        } catch (IOException e) {
            String err = "IOException with client-portal connection.";
            System.out.println(err);
        }
    }
}
