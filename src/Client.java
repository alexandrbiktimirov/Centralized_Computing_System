import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client class demonstrating:
 *  1) UDP broadcast "CCS DISCOVER" to find server
 *  2) Receive "CCS FOUND" + get server IP
 *  3) Connect via TCP to send requests
 */
public class Client {

    /**
     * Entry point for the Client:
     *  Usage: java Client <port>
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Client <port>");
            return;
        }

        int port;

        try {
            port = Integer.parseInt(args[0]);
            if (port < 1024 || port > 65535) {
                System.err.println("Port must be between 1024 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer");
            return;
        }

        InetAddress serverAddress = discoverServer(port);
        if (serverAddress == null) {
            System.err.println("Server not found via broadcast. Exiting.");
            return;
        }

        System.out.println("Server discovered at: " + serverAddress.getHostAddress());

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out   = new PrintWriter(socket.getOutputStream(), true))
        {
            System.out.println("Connected to server. Type requests in form '<OPER> <ARG1> <ARG2>' or 'quit' to exit.");
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Request> ");
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("quit")) {
                    break;
                }

                out.println(line);

                String response = in.readLine();

                if (response == null) {
                    System.out.println("Server closed connection.");
                    break;
                }

                System.out.println("Response: " + response);
            }

            System.out.println("Client terminating.");
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a UDP broadcast "CCS DISCOVER" on the given port
     * Waits for "CCS FOUND" response
     * Returns the server's IP address if discovered, or null if not.
     */
    private static InetAddress discoverServer(int port) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(3000);

            byte[] sendData = "CCS DISCOVER".getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    InetAddress.getByName("255.255.255.255"),
                    port
            );
            socket.send(sendPacket);

            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

            while (true) {
                socket.receive(receivePacket);
                String message = new String(
                        receivePacket.getData(),
                        0,
                        receivePacket.getLength(),
                        "UTF-8"
                );

                if ("CCS FOUND".equals(message)) {
                    return receivePacket.getAddress();
                }
            }
        } catch (SocketTimeoutException e) {

            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}