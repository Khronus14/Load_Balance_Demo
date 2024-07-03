package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerFarm_TCP {
    private int serverCount;
    public int portalPort;
    private final ServerThread_TCP[] servers;
    public static boolean UP = true;
    public static boolean VERBOSE = true;
    public String err;

    public ServerFarm_TCP(String[] args) {
        parseCLA(args);
        this.servers = new ServerThread_TCP[serverCount];
    }

    public static void main(String[] args) {
        ServerFarm_TCP serverFarm = new ServerFarm_TCP(args);
        serverFarm.startServers();
        System.out.println("Program complete.");
    }

    /**
     * Handles command line arguments.
     * @param args command line arguments
     */
    private void parseCLA(String[] args) {
        if (args.length != 2) {
            err = "Invalid number of arguments.";
            usageAndExit(err, true);
        }
        try {
            this.serverCount = Integer.parseInt(args[0]);
            this.portalPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException msg) {
            err = "Invalid type in argument.";
            usageAndExit(err, true);
        }

    }

    /**
     * Function to handle errors.
     * @param message general error message
     * @param userError true iff invalid user input caused error
     */
    public static void usageAndExit(String message, boolean userError) {
        System.err.println(message);
        System.err.println("Usage: java TCP.LB_ServerFarm_TCP [number of servers]" +
                " [portal port]");
        System.exit(userError ? 0 : 1);
    }

    /**
     * Starts program.
     */
    private void startServers() {
        // count is added to client thread name
        System.out.println("Starting servers...");

        for (int count = 0; count < this.serverCount; count++) {
            ServerThread_TCP newServer = new ServerThread_TCP(this.portalPort, count + 1);
            newServer.start();
            this.servers[count] = newServer;
        }

        try (BufferedReader stdIn = new BufferedReader(new
                InputStreamReader(System.in))) {
            // checking for input commands while running
            while (UP) {
                String cmd = stdIn.readLine();
                parseCMD(cmd);
            }

            // wait for servers to disconnect before ending simulation
            for (ServerThread_TCP server : this.servers) {
                server.join();
            }
        } catch (InterruptedException msg) {
            err = "Waiting for servers to stop.";
            usageAndExit(err, false);
        } catch (IOException msg) {
            err = "Input process error.";
            usageAndExit(err, false);
        }

    }

    /**
     * Utility function to parse and execute user input during execution.
     * @param cmd user CLI
     */
    private void parseCMD(String cmd) {
        String[] command = cmd.split(" ");
        try {
            switch (command[0]) {
                // toggle client's finishing messages
                case "!" -> {
                    if (VERBOSE) {
                        VERBOSE = false;
                        System.out.println("Server feedback is turned off.");
                    } else {
                        VERBOSE = true;
                        System.out.println("Server feedback is turned on.");
                    }
                }
                // stops creating clients, then exits once all connections are closed
                case "exit" -> {
                    UP = false;
                    System.out.println("Shutting down servers...");
                }
                default -> System.out.println("Invalid command.\n'!', 'exit'");
            }
        } catch (NumberFormatException msg) {
            System.out.println("Invalid command.\n'!', 'exit'");
        }
    }
}
