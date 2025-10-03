import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ATMClient {

    private static void printMenu() {
        System.out.println("\n========= ATM MENU =========");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit Money");
        System.out.println("3. Withdraw Money");
        System.out.println("4. Logout");
        System.out.println("5. Exit");
        System.out.println("============================");
        System.out.print("Choose an option: ");
    }

    private static String lastServerResponse = "";

    private static boolean readServerResponse(BufferedReader in) throws IOException {
        String line;
        StringBuilder fullResponse = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) break;
            fullResponse.append(line).append("\n");
            System.out.println(">> " + line);
        }
        lastServerResponse = fullResponse.toString();
        return lastServerResponse.contains("LOGIN OK");
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5555);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // Read initial welcome message
            readServerResponse(in);

            boolean loggedIn = false;

            while (true) {
                if (!loggedIn) {
                    System.out.println("\n--- LOGIN ---");
                    System.out.print("Enter Account Number (or type EXIT to quit): ");
                    String account = scanner.nextLine().trim();

                    if (account.equalsIgnoreCase("EXIT")) {
                        System.out.println("Thank you for using ATM. Goodbye!");
                        out.println("EXIT"); // Tell server to exit
                        readServerResponse(in);
                        break; // exit program
                    }

                    System.out.print("Enter PIN: ");
                    String pin = scanner.nextLine().trim();

                    if (pin.equalsIgnoreCase("EXIT")) {
                        System.out.println("Thank you for using ATM. Goodbye!");
                        out.println("EXIT");
                        readServerResponse(in);
                        break;
                    }

                    out.println("LOGIN " + account + " " + pin);
                    loggedIn = readServerResponse(in);

                    if (!loggedIn) {
                        System.out.println("Login failed. Try again or type EXIT to quit.");
                    }
                } else {
                    printMenu();
                    String choice = scanner.nextLine().trim();

                    switch (choice) {
                        case "1":
                            out.println("BAL");
                            break;
                        case "2":
                            System.out.print("Enter amount to deposit: ");
                            String dep = scanner.nextLine().trim();
                            out.println("DEP " + dep);
                            break;
                        case "3":
                            System.out.print("Enter amount to withdraw: ");
                            String wdr = scanner.nextLine().trim();
                            out.println("WDR " + wdr);
                            break;
                        case "4":
                            out.println("LOGOUT");
                            readServerResponse(in);
                            loggedIn = false;
                            break;
                        case "5":
                            out.println("EXIT");
                            readServerResponse(in);
                            System.out.println("Thank you for using ATM. Goodbye!");
                            return;
                        default:
                            System.out.println("Invalid choice. Please select a valid option.");
                            continue;
                    }
                    if (loggedIn) readServerResponse(in);
                }
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
