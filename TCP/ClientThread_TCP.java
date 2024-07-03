package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class ClientThread_TCP extends Thread {
    public String portalAddr;
    public int portalPort;
    private int curCount = 0;
    public final int count;
    private final Client_Generator_TCP gen;
    public ClientThread_TCP(String portalAddr, int portalPort, int count, Client_Generator_TCP gen) {
        super("Client " + count);
        this.portalAddr = portalAddr;
        this.portalPort = portalPort;
        this.count = Client_Generator_TCP.rng.nextInt(200, 500);
        this.gen = gen;
    }

    /**
     * Client logic.
     */
    public void run() {
        //initial message
        String toPortal = String.format("%d %d %d", 0, 0, this.count);
        String fromPortal;
        boolean counting = true;
        long startTime = System.nanoTime();

        try (Socket socket = new Socket(this.portalAddr, this.portalPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            // send initial message
            out.println(toPortal);

            // loop until count limit met
            while (counting) {
                fromPortal = in.readLine();

                // done counting
                if (fromPortal.startsWith(LB_Protocol.COMPLETE)) {
                    this.curCount++;
                    counting = false;
                }

                // in process of counting
                if (fromPortal.startsWith(LB_Protocol.COUNTING)) {
                    this.curCount++;
                    toPortal = fromPortal;
                    out.println(toPortal);
                } else if (fromPortal.startsWith(LB_Protocol.INITIAL_REQUEST)) {
                    String err = "Initial message returned from portal";
                    Client_Generator_TCP.usageAndExit(err, false);
                }
            }
        } catch (UnknownHostException e) {
            String err = this.getName() + " tried to connect to an unknown host.";
            System.out.println(err);
        } catch (IOException e) {
            String err = "IO Exception in client for " + this.portalAddr;
            System.out.println(err);
        }

        gen.updateClients(-1);
        // print this client's stats if VERBOSE
        if (Client_Generator_TCP.VERBOSE) {
            long totalTime = (System.nanoTime() - startTime) / 1000000000;
            System.out.printf("%s counted to %d out of %d in %d seconds.\n",
                    this.getName(), this.curCount, this.count, totalTime);
        }
    }
}
