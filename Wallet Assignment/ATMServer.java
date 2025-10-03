import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ATMServer {

    private final Map<String, Integer> accounts = new ConcurrentHashMap<>();
    private final Map<String, String> pins = new HashMap<>();
    private final int port;

    public ATMServer(int port) {
        this.port = port;
        seedAccounts();
    }

    private void seedAccounts() {
        accounts.put("1001", 1000);
        accounts.put("1002", 500);
        accounts.put("1003", 250);

        pins.put("1001", "1234");
        pins.put("1002", "0000");
        pins.put("1003", "4321");
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("ATM Server started on port " + port);
        ExecutorService pool = Executors.newCachedThreadPool();

        while (true) {
            Socket client = serverSocket.accept();
            pool.submit(() -> handleClient(client));
        }
    }

    private void handleClient(Socket socket) {
        String currentAccount = null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            sendMessage(out, "Welcome to ATM!", "Please login using: LOGIN <account> <PIN>");

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.trim().split(" ");
                String command = parts[0].toUpperCase();

                if (currentAccount == null) {
                    if (command.equals("LOGIN") && parts.length == 3) {
                        String acc = parts[1];
                        String pin = parts[2];

                        if (accounts.containsKey(acc) && pins.get(acc).equals(pin)) {
                            currentAccount = acc;
                            sendMessage(out,
                                    "LOGIN OK. Welcome, account " + currentAccount + "!",
                                    "Available commands: BAL | DEP <amount> | WDR <amount> | LOGOUT | EXIT"
                            );
                        } else {
                            sendMessage(out, "ERROR: Invalid account or PIN");
                        }
                    } else {
                        sendMessage(out, "ERROR: Please login first with LOGIN <account> <PIN>");
                    }
                } else {
                    switch (command) {
                        case "BAL":
                            sendMessage(out, "Your balance is: " + accounts.get(currentAccount));
                            break;

                        case "DEP":
                            if (parts.length == 2) {
                                try {
                                    int amt = Integer.parseInt(parts[1]);
                                    accounts.put(currentAccount, accounts.get(currentAccount) + amt);
                                    sendMessage(out, "Deposited " + amt + ". New balance: " + accounts.get(currentAccount));
                                } catch (NumberFormatException e) {
                                    sendMessage(out, "ERROR: Invalid amount. Please enter a number.");
                                }
                            } else {
                                sendMessage(out, "ERROR: Invalid deposit command. Usage: DEP <amount>");
                            }
                            break;

                        case "WDR":
                            if (parts.length == 2) {
                                try {
                                    int amt = Integer.parseInt(parts[1]);
                                    if (accounts.get(currentAccount) >= amt) {
                                        accounts.put(currentAccount, accounts.get(currentAccount) - amt);
                                        sendMessage(out, "Withdrew " + amt + ". New balance: " + accounts.get(currentAccount));
                                    } else {
                                        sendMessage(out, "ERROR: Insufficient funds.");
                                    }
                                } catch (NumberFormatException e) {
                                    sendMessage(out, "ERROR: Invalid amount. Please enter a number.");
                                }
                            } else {
                                sendMessage(out, "ERROR: Invalid withdraw command. Usage: WDR <amount>");
                            }
                            break;

                        case "LOGOUT":
                            sendMessage(out,
                                    "You have successfully logged out from account " + currentAccount + ".",
                                    "Please login again using: LOGIN <account> <PIN>"
                            );
                            currentAccount = null;
                            break;

                        case "EXIT":
                            sendMessage(out, "Goodbye!");
                            socket.close();
                            return;

                        default:
                            sendMessage(out, "ERROR: Unknown command.",
                                    "Available commands: BAL | DEP <amount> | WDR <amount> | LOGOUT | EXIT");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void sendMessage(PrintWriter out, String... lines) {
        for (String line : lines) {
            out.println(line);
        }
        out.println(); // empty line to mark end of message
    }

    public static void main(String[] args) throws IOException {
        ATMServer server = new ATMServer(5555);
        server.start();
    }
}
