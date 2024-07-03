package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Random;

public class Client_Generator_TCP {
    public int curClients = 0;
    public static int maxClients;
    public String portalAddr;
    public int portalPort;
    private final LinkedList<ClientThread_TCP> clients;
    public static boolean UP = true;
    public static boolean VERBOSE = false;
    public static final Random rng = new Random();
    public static String err = "Error";

    public Client_Generator_TCP(String[] args) {
        parseCLA(args);
        this.clients = new LinkedList<>();
    }

    public static void main(String[] args) {
        Client_Generator_TCP clientGen = new Client_Generator_TCP(args);
        clientGen.genClients();
        System.out.println("Program complete.");
    }

    /**
     * Handles command line arguments.
     * @param args command line arguments
     */
    private void parseCLA(String[] args) {
        if (args.length != 3) {
            err = "Invalid number of arguments.";
            usageAndExit(err, true);
        }
        try {
            maxClients = Integer.parseInt(args[0]);
            this.portalAddr = args[1];
            this.portalPort = Integer.parseInt(args[2]);
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
        System.err.println("Usage: java Client_Generator [number of clients]" +
                " [portal address] [portal port]");
        System.exit(userError ? 0 : 1);
    }

    /**
     * Starts program.
     */
    private void genClients() {
        // create a thread to handle commands
        Thread inputThread = new Thread("ClientInput") {
            public void run() {
                try (BufferedReader stdIn = new BufferedReader(new
                        InputStreamReader(System.in))) {
                    while (UP) {
                        String cmd = stdIn.readLine();
                        parseCMD(cmd);
                    }
                } catch (IOException msg) {
                    err = "Input process thread error.";
                    usageAndExit(err, false);
                }
            }
        };
        inputThread.start();

        // count is added to client thread name
        int count = 0;
        System.out.println("Generating clients...");

        // monitor current client count, create new client if below target count
        while (UP) {
            if (curClients < maxClients) {
                count++;
                this.addClient(count);
            }

            // added sleep here to avoid blocking issue on creating new clients
            try {
                //TODO troubleshoot blocking issue when sleep is removed
                Thread.sleep(10);
            } catch (InterruptedException msg) {
                err = "Main thread interrupted during client generation.";
                usageAndExit(err, false);
            }
        }

        // wait for clients to disconnect before ending simulation
        for (ClientThread_TCP client : this.clients) {
            try {
                client.join();
            } catch (InterruptedException msg) {
                err = "Waiting for clients to stop.";
                usageAndExit(err, false);
            }
        }
        System.out.println("Created " + count + " unique clients.");
    }

    /**
     * Helper function to create a new client.
     * @param count the [count] client to be created this session
     */
    private void addClient(int count) {
        ClientThread_TCP newClient = new ClientThread_TCP(this.portalAddr,
                this.portalPort, count, this);
        newClient.start();
        this.clients.add(newClient);
        this.updateClients(1);
    }

    /**
     * Utility function to parse and execute user input during execution.
     * @param cmd user CLI
     */
    private void parseCMD(String cmd) {
        String[] command = cmd.split(" ");
        try {
            switch (command[0]) {
                // increase target client number
                case "add" -> {
                    maxClients += Integer.parseInt(command[1]);
                    if (maxClients < 0) maxClients = 0;
                    System.out.println("Target clients is set to " + maxClients);
                }
                // decrease target client number
                case "remove" -> {
                    maxClients -= Integer.parseInt(command[1]);
                    if (maxClients < 0) maxClients = 0;
                    System.out.println("Target clients is set to " + maxClients);
                }
                // print client stats
                case "?" -> System.out.println("Target clients: " + maxClients + "\n" +
                        curClients + " clients active.");
                // toggle client's finishing messages
                case "!" -> {
                    if (VERBOSE) {
                        VERBOSE = false;
                        System.out.println("Client feedback is turned off.");
                    } else {
                        VERBOSE = true;
                        System.out.println("Client feedback is turned on.");
                    }
                }
                // stops creating clients, then exits once all connections are closed
                case "exit" -> {
                    UP = false;
                    System.out.println("Shutting down...");
                }
                default -> System.out.println("Invalid command.\n'add [int]', 'remove [int]', '!', 'exit'");
            }
        } catch (NumberFormatException msg) {
            System.out.println("Invalid command.\n'add [int]', 'remove [int]', '!', 'exit'");
        }
    }

    public synchronized void updateClients(int value) {
        curClients = curClients + value;
    }
}
