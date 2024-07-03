package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Portal_HealthConn_TCP extends Thread {
    public final String serverAddr;
    public final int serverPort;
    private final int index;
    private final Portal_TCP portal;
    public Portal_HealthConn_TCP(String serverAddr, int serverPort, int count, Portal_TCP portal) {
        super("PortalHealth " + (count + 1));
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.index = count;
        this.portal = portal;
    }

    /**
     * Client logic.
     */
    public void run() {
        // health status request message
        String toServer = String.format("%s %d", LB_Protocol.HEALTH, this.portal.serverStats[this.index][0]);
        String fromServer;

        try (Socket socket = new Socket(this.serverAddr, this.serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            // send initial message
            out.println(toServer);

            // loop forever
            while (this.portal.UP) {
                fromServer = in.readLine();

                // process health status
                if (fromServer.startsWith(LB_Protocol.HEALTH)) {
                    String[] _fromServer = fromServer.split(" ");
                    this.portal.serverStats[this.index][0] = Integer.parseInt(_fromServer[1]);
                    this.portal.serverStats[this.index][1] = Integer.parseInt(_fromServer[2]);
                    this.portal.serverStats[this.index][2] = Integer.parseInt(_fromServer[3]);
                }

                // wait 4.5 seconds and send next request
                Thread.sleep(250);

                // use current status of server
                toServer = String.format("%s %d", LB_Protocol.HEALTH,
                        this.portal.serverStats[this.index][0]);
                out.println(toServer);
            }
        } catch (UnknownHostException e) {
            String err = this.getName() + " tried to connect to an unknown host.";
            System.out.println(err);
        } catch (IOException e) {
            String err = "IO Exception in client for " + this.serverAddr;
            System.out.println(err);
        } catch (InterruptedException msg) {
            String err = this.getName() + " interrupted sleep.";
            System.out.println(err);
        }
    }
}
