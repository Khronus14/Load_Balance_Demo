package TCP;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerThread_TCP extends Thread {
    private final int port;
    private static final long PRINT_STATS = 3000000000L;
    public int connections = 0;
    public int usage = 0;
    public ServerThread_TCP(int port, int count) {
        super("Server " + count);
        this.port = port + count;
    }

    public void run() {
        // separate thread to print portal stats
        Thread printThread = new Thread(this.getName()) {
            public void run() {
                long updateTimer = System.nanoTime();
                while (ServerFarm_TCP.UP) {
                    if ((System.nanoTime() - updateTimer) > PRINT_STATS) {
                        System.out.println(this.getName() + "'s number of " +
                                "connections: " + connections);
                        updateTimer = System.nanoTime();
                    }
                }
            }
        };
        printThread.start();

        System.out.println(this.getName() + " online; awaiting clients...");

        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            // first connection is health connection to portal
            new ServerFarm_HealthConn_TCP(serverSocket.accept(), this).start();

            while (ServerFarm_TCP.UP) {
                // create and start a new thread for each client connection
                new ServerFarm_Connection_TCP(serverSocket.accept(), this).start();
            }
        } catch (IOException e) {
            String err = "Could not listen on port " + this.port;
            Client_Generator_TCP.usageAndExit(err, false);
        }
    }

    public synchronized void updateConnections(int value) {
        this.connections = this.connections + value;
    }

    public synchronized void updateUsage(int value) {
        this.usage = this.usage + value;
    }
}
