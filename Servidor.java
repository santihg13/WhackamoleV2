import javax.jms.*;
import javax.naming.InitialContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor implements MessageListener {
    private static final int SERVER_PORT = 1234;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicPublisher topicPublisher;

    private int currentPlayerIndex = 0;
    private int totalPlayers = 0;
    private int scoreThreshold = 5;
    private int currentScore = 0;
    private int currentTopoRow = -1;
    private int currentTopoCol = -1;

    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Servidor iniciado en el puerto " + SERVER_PORT);

            // Configurar la conexión JMS para enviar mensajes a los jugadores
            InitialContext context = new InitialContext();
            TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) context.lookup("ConnectionFactory");
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = topicSession.createTopic("Whack-a-Mole");
            topicPublisher = topicSession.createPublisher(topic);
            topicConnection.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToAllPlayers(String message) {
        try {
            TextMessage textMessage = topicSession.createTextMessage(message);
            topicPublisher.publish(textMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void sendToPlayer(String playerName, String message) {
        try {
            TextMessage textMessage = topicSession.createTextMessage(message);
            textMessage.setStringProperty("JMS_IBM_Character_Set", "UTF-8");
            textMessage.setStringProperty("JMS_IBM_Encoding", "546");
            topicPublisher.publish(textMessage, DeliveryMode.PERSISTENT, 4, 0);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;
        private String playerName;

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
                playerName = input.readLine();
                if (playerName == null || playerName.trim().isEmpty()) {
                    System.out.println("Nombre inválido.");
                    clientSocket.close();
                    return;
                }

                System.out.println("Jugador registrado: " + playerName);
                output.println("Registro exitoso. ¡Bienvenido al juego!");

                totalPlayers++;
                if (totalPlayers == 1) {
                    sendToPlayer(playerName, "GAME_START");
                }

                if (totalPlayers > 1) {
                    sendToPlayer(playerName, "Otro jugador se ha unido. El juego comenzará pronto.");
                }

                // Esperar a que se unan más jugadores o alcanzar la puntuación límite
                while (currentScore < scoreThreshold) {
                    String message = input.readLine();
                    if (message == null) {
                        break;
                    }

                    String[] parts = message.split(" ");
                    String messageType = parts[0];

                    if (messageType.equals("TOPO_POSITION")) {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        if (row == currentTopoRow && col == currentTopoCol) {
                            currentScore++;
                            sendToAllPlayers("TOPIC_HIT " + row + " " + col);
                            if (currentScore >= scoreThreshold) {
                                sendToAllPlayers("GAME_END " + playerName);
                            }
                        }
                    } else if (messageType.equals("TOPO_HIT")) {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        if (row == currentTopoRow && col == currentTopoCol) {
                            currentScore++;
                            sendToAllPlayers("TOPIC_HIT " + row + " " + col);
                            if (currentScore >= scoreThreshold) {
                                sendToAllPlayers("GAME_END " + playerName);
                            }
                        }
                    }
                }

                totalPlayers--;
                clients.remove(this);
                input.close();
                output.close();
                clientSocket.close();
                System.out.println("Jugador desconectado: " + playerName);

                if (totalPlayers == 0) {
                    currentScore = 0;
                    sendToAllPlayers("GAME_END");
                } else if (currentPlayerIndex >= totalPlayers) {
                    currentPlayerIndex = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
