import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;

public class CCS {
    //Variables for global statistics
    private static final AtomicInteger totalClients = new AtomicInteger(0);
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger totalAddOps = new AtomicInteger(0);
    private static final AtomicInteger totalSubOps = new AtomicInteger(0);
    private static final AtomicInteger totalMulOps = new AtomicInteger(0);
    private static final AtomicInteger totalDivOps = new AtomicInteger(0);
    private static final AtomicInteger totalErrors = new AtomicInteger(0);
    private static final AtomicLong   totalSumOfResults = new AtomicLong(0);

    //Variables for statistics for the last 10 seconds
    private static int lastClients = 0;
    private static int lastRequests = 0;
    private static int lastAddOps = 0;
    private static int lastSubOps = 0;
    private static int lastMulOps = 0;
    private static int lastDivOps = 0;
    private static int lastErrors = 0;
    private static long lastSumOfResults = 0;

    private static volatile boolean running = true;

    /**
     * createUdpDiscoveryThread(int port) handles the UDP service discovery. It:
     * 1) Listens on <port> for "CCS DISCOVER"
     * 2) Replies "CCS FOUND"
     */
    private static Thread createUdpDiscoveryThread(int port) {
        return new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                byte[] buffer = new byte[1024];

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(
                            packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8
                    );

                    if (received.startsWith("CCS DISCOVER")) {
                        byte[] response = "CCS FOUND".getBytes(StandardCharsets.UTF_8);
                        DatagramPacket respPacket = new DatagramPacket(
                                response,
                                response.length,
                                packet.getAddress(),
                                packet.getPort()
                        );
                        socket.send(respPacket);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("UDP discovery error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * createTcpAcceptThread(int port) accepts incoming TCP clients and spawns a worker thread for each.
     */
    private static Thread createTcpAcceptThread(int port) {
        return new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (running) {
                    Socket clientSocket = serverSocket.accept();

                    totalClients.incrementAndGet();

                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("TCP accept error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * ClientHandler is a runnable private class that handles a single client's requests
     * in a loop until the client disconnects.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String line;

                while ((line = in.readLine()) != null) {
                    String response = handleRequest(line);
                    out.println(response);
                }
            } catch (IOException e) {

            } finally {
                try {
                    socket.close();
                } catch (IOException e) {

                }
            }
        }

        private String handleRequest(String requestLine) {
            totalRequests.incrementAndGet();

            System.out.println("Received request: " + requestLine);

            String[] parts = requestLine.trim().split("\\s+");
            if (parts.length != 3) {
                totalErrors.incrementAndGet();
                System.out.println("Result: ERROR");
                return "ERROR";
            }

            String oper = parts[0];
            int arg1, arg2;

            try {
                arg1 = Integer.parseInt(parts[1]);
                arg2 = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                totalErrors.incrementAndGet();
                System.out.println("Result: ERROR");
                return "ERROR";
            }

            long result;
            switch (oper) {
                case "ADD":
                    totalAddOps.incrementAndGet();
                    result = (long) arg1 + arg2;
                    break;
                case "SUB":
                    totalSubOps.incrementAndGet();
                    result = (long) arg1 - arg2;
                    break;
                case "MUL":
                    totalMulOps.incrementAndGet();
                    result = (long) arg1 * arg2;
                    break;
                case "DIV":
                    totalDivOps.incrementAndGet();
                    if (arg2 == 0) {
                        totalErrors.incrementAndGet();
                        System.out.println("Result: ERROR (division by zero)");
                        return "ERROR";
                    }
                    result = (long) arg1 / arg2;
                    break;
                default:
                    totalErrors.incrementAndGet();
                    System.out.println("Result: ERROR (invalid operation)");
                    return "ERROR";
            }

            totalSumOfResults.addAndGet(result);

            System.out.println("Result: " + result);

            return String.valueOf(result);
        }
    }

    /**
     * createStatsThread() prints statistics every 10 seconds. It prints:
     * 1) Stats since start
     * 2) Stats from the last 10 seconds
     * And then it resets "last 10 seconds" counters.
     */
    private static Thread createStatsThread() {
        return new Thread(() -> {
            long lastGlobalClients = 0;
            long lastGlobalRequests = 0;
            long lastGlobalAddOps = 0;
            long lastGlobalSubOps = 0;
            long lastGlobalMulOps = 0;
            long lastGlobalDivOps = 0;
            long lastGlobalErrors = 0;
            long lastGlobalSumOfResults = 0;

            while (running) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {

                }

                long gc = totalClients.get();
                long gr = totalRequests.get();
                long ga = totalAddOps.get();
                long gs = totalSubOps.get();
                long gm = totalMulOps.get();
                long gd = totalDivOps.get();
                long ge = totalErrors.get();
                long sr = totalSumOfResults.get();

                System.out.println("=== Statistics (global since start) ===");
                System.out.println("Connected clients:          " + gc);
                System.out.println("Requests processed:         " + gr);
                System.out.println("   ADD operations:          " + ga);
                System.out.println("   SUB operations:          " + gs);
                System.out.println("   MUL operations:          " + gm);
                System.out.println("   DIV operations:          " + gd);
                System.out.println("Errors:                     " + ge);
                System.out.println("Sum of computed results:    " + sr);

                System.out.println("=== Statistics (last 10s) ===");
                lastClients = (int) (gc - lastGlobalClients);
                lastRequests = (int) (gr - lastGlobalRequests);
                lastAddOps = (int) (ga - lastGlobalAddOps);
                lastSubOps= (int) (gs - lastGlobalSubOps);
                lastMulOps = (int) (gm - lastGlobalMulOps);
                lastDivOps = (int) (gd - lastGlobalDivOps);
                lastErrors = (int) (ge - lastGlobalErrors);
                lastSumOfResults = sr - lastGlobalSumOfResults;

                System.out.println("New clients:                " + lastClients);
                System.out.println("Requests processed:         " + lastRequests);
                System.out.println("   ADD operations:          " + lastAddOps);
                System.out.println("   SUB operations:          " + lastSubOps);
                System.out.println("   MUL operations:          " + lastMulOps);
                System.out.println("   DIV operations:          " + lastDivOps);
                System.out.println("Errors:                     " + lastErrors);
                System.out.println("Sum of computed results:    " + lastSumOfResults);
                System.out.println("========================================\n");

                lastGlobalClients = gc;
                lastGlobalRequests = gr;
                lastGlobalAddOps = ga;
                lastGlobalSubOps = gs;
                lastGlobalMulOps = gm;
                lastGlobalDivOps = gd;
                lastGlobalErrors = ge;
                lastGlobalSumOfResults = sr;
            }
        }, "Stats-Thread");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar CCS.jar <port>");
            return;
        }

        int port;

        try {
            port = Integer.parseInt(args[0]);

            if (port < 1024 || port > 65535) {
                System.err.println("Port must be between 1024 and 65535, because others are reserved for the system");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer");
            return;
        }

        Thread udpThread = createUdpDiscoveryThread(port);
        Thread tcpThread = createTcpAcceptThread(port);
        Thread statsThread = createStatsThread();

        udpThread.start();
        tcpThread.start();
        statsThread.start();

        System.out.println("Server started on port " + port + ". UDP/TCP for discovery/requests");
    }
}