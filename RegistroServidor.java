import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class RegistroServidor {
    private static final int SERVER_PORT = 5678;

    private ServerSocket serverSocket;
    private Map<String, String> players = new HashMap<>();

    public static void main(String[] args) {
        RegistroServidor registroServidor = new RegistroServidor();
        registroServidor.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Servidor de registro iniciado en el puerto " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String playerName = input.readLine();
                System.out.println("Jugador registrado: " + playerName);

                if (playerName == null || playerName.trim().isEmpty()) {
                    output.println("");
                } else {
                    String serverAddress = players.get(playerName);
                    if (serverAddress == null || serverAddress.isEmpty()) {
                        serverAddress = clientSocket.getInetAddress().getHostAddress();
                        players.put(playerName, serverAddress);
                        System.out.println("Dirección IP del servidor de juego para " + playerName + ": " + serverAddress);
                    }

                    output.println(serverAddress);
                }

                input.close();
                output.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
