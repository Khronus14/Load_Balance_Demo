package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

public class Portal_TCP {
    private final long PRINT_STATS = 5000000000L;
    private long scaleTimer;
    private int serverTotal;
    private int serverTempMax;
    private int activeServers;
    /** serverStats:
     *  [server][status, connections, usage, hard fail, minConn, maxConn, weightedValue]
     */
    public int[][] serverStats;
    private String serverAddr;
    private int portalPort;
    public boolean UP = true;
    private boolean wrr = false;
    private int[] wrrNext;
    private boolean lc = true;
    public int MIN_CONN_THRESHOLD = 20;
    public int MAX_CONN_THRESHOLD = 80;
    private static final String tableHeader = """
            *** Server status
                Server       | Status       | Connections    | Usage
            """;
    private static final String tableEntry = """
                %-10d   | %-10s   | %-10d     | %d
            """;

    public Portal_TCP(String[] args) {
        parseCLA(args);
    }
    public static void main(String[] args) {
        Portal_TCP portal = new Portal_TCP(args);
        portal.startPortal();
    }

    /**
     * Handles command line arguments.
     * @param args command line arguments
     */
    private void parseCLA(String[] args) {
        if (args.length != 3) {
            String err = "Invalid number of arguments.";
            Client_Generator_TCP.usageAndExit(err, true);
        }
        try {
            this.serverTotal = Integer.parseInt(args[0]);
            this.serverTempMax = this.serverTotal;
            this.serverAddr = args[1];
            this.portalPort = Integer.parseInt(args[2]);
            this.serverStats = new int[this.serverTotal][7];
            for (int[] server : this.serverStats) {
                server[4] = this.MIN_CONN_THRESHOLD;
                server[5] = this.MAX_CONN_THRESHOLD;
                server[6] = 1;
            }
            this.serverStats[0][0] = 1;
            this.wrrNext = new int[this.serverTotal];
            this.activeServers = 1;
        } catch (NumberFormatException msg) {
            String err = "Invalid type in argument.";
            Client_Generator_TCP.usageAndExit(err, true);
        }
    }

    private void startPortal() {
        // separate thread to print portal stats
        Thread printThread = new Thread("PortalPrint") {
            public void run() {
                long updateTimer = System.nanoTime();
                while (UP) {
                    if ((System.nanoTime() - updateTimer) > PRINT_STATS) {
                        printTable();
                        updateTimer = System.nanoTime();
                    }
                }
            }
        };
        printThread.start();

        // create a thread to handle commands
        Thread inputThread = new Thread("PortalInput") {
            public void run() {
                try (BufferedReader stdIn = new BufferedReader(new
                        InputStreamReader(System.in))) {
                    while (UP) {
                        String cmd = stdIn.readLine();
                        parseCMD(cmd);
                    }
                } catch (IOException msg) {
                    String err = "Input process thread error.";
                    Client_Generator_TCP.usageAndExit(err, false);
                }
            }
        };
        inputThread.start();

        // create portal-server connections for health updates
        for (int count = 0; count < this.serverTotal; count++) {
            int tempPort = this.portalPort + 1 + count;
            Portal_HealthConn_TCP newHealth = new Portal_HealthConn_TCP(this.serverAddr,
                    tempPort, count, this);
            newHealth.start();
        }

        System.out.println("Portal online; awaiting clients...");


        try (ServerSocket serverSocket = new ServerSocket(this.portalPort)) {
            serverSocket.setSoTimeout(100);
            this.scaleTimer = System.nanoTime();
            while (this.UP) {
                boolean scaleUp = this.activeServers < this.serverTempMax;
                boolean scaleDown = this.activeServers > 1;

                // scheduling based on method
                int newPort = 0;
                int server = 0;
                if (wrr) {
                    // wrr
                    boolean looking = true;
                    while (looking) {
                        for (int index = 0; index < this.wrrNext.length; index++) {
                            if (this.serverStats[index][0] == 1 &&
                                    this.wrrNext[index] < this.serverStats[index][6]) {
                                newPort = this.portalPort + index + 1;
                                this.wrrNext[index]++;
                                server = index;
                                looking = false;
                                break;
                            }
                        }
                        // if a round is complete, reset tracker and go again
                        if (newPort == 0) this.wrrNext = new int[this.serverTotal];
                    }
                } else {
                    // lc; need to ensure we only check against active servers
                    int leastCon = 0;
                    for (int serverA = 0; serverA < this.serverStats.length; serverA++) {
                        if (this.serverStats[serverA][0] == 1) {
                            for (int serverB = serverA + 1; serverB < this.serverStats.length; serverB++) {
                                if (this.serverStats[serverB][0] == 1) {
                                    if (this.serverStats[serverB][1] < this.serverStats[leastCon][1]) {
                                        leastCon = serverB;
                                    }
                                }
                            }
                        }
                    }
                    newPort = this.portalPort + leastCon + 1;
                    server = leastCon;
                }

                //TODO scheduling is based on delayed connection totals; it would
                // be better to adjust connection totals here in between health updates

                // create new connection based on scheduler decision
                try {
                    new Portal_Connection_TCP(serverSocket.accept(), this.serverAddr,
                            newPort, server, this).start();
                } catch (SocketTimeoutException msg) {
                    // no client request was present
                }

                // scaling check
                this.checkScaling(scaleUp, scaleDown);
            }
        } catch (IOException e) {
            String err = "Could not listen on port " + this.portalPort;
            Client_Generator_TCP.usageAndExit(err, false);
        }
    }

    private void checkScaling(boolean scaleUp, boolean scaleDown) {
        // check if we need to scale
        for (int[] server : this.serverStats) {
            if (server[3] == 0 && server[0] == 1) {
                if (scaleUp && server[1] < server[5]) {
                    scaleUp = false;
                }
                if (scaleDown && server[1] > server[4]) {
                    scaleDown = false;
                }
            }
        }

        // bring server up/down as necessary
        if (scaleUp) {
            for (int[] server : this.serverStats) {
                if (server[3] == 0 && server[0] == 0) {
                    server[0] = 1;
                    this.activeServers++;
                    break;
                }
            }
        } else if (scaleDown && (System.nanoTime() - scaleTimer > 80000000000L)) {
            for (int[] server : this.serverStats) {
                if (server[3] == 0 && server[0] == 1) {
                    server[0] = 0;
                    this.activeServers--;
                    scaleTimer = System.nanoTime();
                    break;
                }
            }
        }
    }

    /**
     * Utility function to print the routing table.
     */
    private void printTable() {
        StringBuilder tableStr = new StringBuilder();
        tableStr.append(String.format(tableHeader));
        for (int index = 0; index < this.serverStats.length; index++) {
            String status;
            int conn, usage;
            if (this.serverStats[index][3] == 1) {
                status = "failure";
                conn = 0;
                usage = 0;
            } else {
                status = (this.serverStats[index][0] == 0) ? "standby" : "active";
                conn = this.serverStats[index][1];
                usage = this.serverStats[index][2];
            }

            tableStr.append(String.format(tableEntry, index + 1, status, conn, usage));
        }
        System.out.println(tableStr);
    }

    /**
     * Utility function to parse and execute user input during execution.
     * @param cmd user CLI
     */
    private void parseCMD(String cmd) {
        String[] command = cmd.split(" ");
        try {
            switch (command[0]) {
                // fail a certain server
                case "down" -> {
                    int server = Integer.parseInt(command[1]);
                    this.serverStats[server - 1][3] = 1;
                    if (this.serverStats[server - 1][0] == 1) {
                        this.serverStats[server - 1][0] = 0;
                        this.activeServers--;
                    }
                    this.serverTempMax--;
                    System.err.println("Server " + server + " in fail mode.");
                }
                // bring a server back up
                case "up" -> {
                    int server = Integer.parseInt(command[1]);
                    if (this.serverStats[server - 1][3] == 1) {
                        this.serverStats[server - 1][3] = 0;
                        this.serverTempMax++;
                        System.err.println("Server " + server + " in standby.");
                    } else {
                        System.err.println("Server " + server + " is already in standby.");
                    }
                }
                // use weighted round-robin for scheduling
                case "wrr" -> {
                    if (!this.wrr) {
                        int server = Integer.parseInt(command[1]);
                        int weight = Integer.parseInt(command[2]);
                        this.serverStats[server - 1][4] *= weight;
                        this.serverStats[server - 1][5] *= weight;
                        this.serverStats[server - 1][6] = weight;
                        this.wrrNext = new int[this.serverTotal];
                        this.wrr = true;
                        this.lc = false;
                        System.err.println("Weighted round-robin now active.");
                    } else {
                        System.err.println("Weighted round-robin already in use.");
                    }
                }
                // use the least connection for scheduling
                case "lc" -> {
                    if (!this.lc) {
                        this.lc = true;
                        this.wrr = false;
                        for (int[] server : this.serverStats) {
                            server[4] = this.MIN_CONN_THRESHOLD;
                            server[5] = this.MAX_CONN_THRESHOLD;
                            server[6] = 1;
                        }
                        System.err.println("Least connection now active.");
                    } else {
                        System.err.println("Least connection already in use.");
                    }
                }
                // print portal stats
                case "?" -> System.out.printf("""
                        *** System status:
                            Maximum server count: %d
                            Standby/active servers: %d
                            Active servers: %d
                        %n""", this.serverTotal, this.serverTempMax, this.activeServers);
                // stops creating clients, then exits once all connections are closed
                case "exit" -> {
                    this.UP = false;
                    System.out.println("Shutting down...");
                }
                default -> System.out.println("Invalid command.\n'down [server]'," +
                        "'up [server]', 'wrr [server] [weight]', 'lc', '?', 'exit'");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException msg) {
            System.out.println("Invalid command.\n'down [server]', 'up [server]'," +
                    "'wrr [server] [weight]', 'lc', '?', 'exit'");
        }
    }
}
