import javax.jms.*;
import javax.naming.InitialContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

public class Cliente implements MessageListener {
    private static final String REGISTRY_SERVER_IP = "localhost";
    private static final int REGISTRY_SERVER_PORT = 5678;
    private static final int SERVER_PORT = 1234;
    private static final int GRID_SIZE = 5;
    private static final int TOPO_DURATION = 1000;
    private static final int TOPO_REAPPEAR_DELAY = 2500;

    private static JFrame frame;
    private static JCheckBox[][] checkboxes;
    private static int currentTopoRow = -1;
    private static int currentTopoCol = -1;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicSubscriber topicSubscriber;

    private String playerName;
    private String serverAddress;

    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.registerPlayer();
    }

    public void registerPlayer() {
        try {
            // Conectar con el servidor de registro
            Socket registrySocket = new Socket(REGISTRY_SERVER_IP, REGISTRY_SERVER_PORT);
            BufferedReader registryInput = new BufferedReader(new InputStreamReader(registrySocket.getInputStream()));
            PrintWriter registryOutput = new PrintWriter(registrySocket.getOutputStream(), true);

            // Obtener nombre del jugador
            playerName = JOptionPane.showInputDialog(frame, "Ingresa tu nombre:");
            if (playerName == null || playerName.trim().isEmpty()) {
                System.out.println("Nombre inválido.");
                registrySocket.close();
                return;
            }

            // Registrar el jugador y obtener la dirección IP del servidor de juego
            registryOutput.println(playerName);
            serverAddress = registryInput.readLine();
            registrySocket.close();

            if (serverAddress == null || serverAddress.isEmpty()) {
                System.out.println("Error al obtener la dirección IP del servidor.");
                return;
            }

            System.out.println("Registrado como " + playerName);
            System.out.println("Dirección IP del servidor: " + serverAddress);

            // Conectar con el servidor de juego
            socket = new Socket(serverAddress, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Iniciar la interfaz gráfica del juego
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("Whack-a-Mole - Jugador: " + playerName);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);
                frame.setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

                checkboxes = new JCheckBox[GRID_SIZE][GRID_SIZE];
                for (int i = 0; i < GRID_SIZE; i++) {
                    for (int j = 0; j < GRID_SIZE; j++) {
                        JCheckBox checkbox = new JCheckBox();
                        checkbox.setEnabled(false);
                        checkboxes[i][j] = checkbox;
                        frame.add(checkbox);
                    }
                }

                frame.setVisible(true);
            });

            // Configurar la conexión JMS para recibir mensajes del servidor
            InitialContext context = new InitialContext();
            TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) context.lookup("ConnectionFactory");
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = topicSession.createTopic(playerName);
            topicSubscriber = topicSession.createSubscriber(topic);
            topicSubscriber.setMessageListener(this);
            topicConnection.start();

            // Iniciar la lógica del juego
            startGameLogic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGameLogic() {
        Timer timer = new Timer();

        // Programar la aparición y desaparición de los topos
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (currentTopoRow != -1 && currentTopoCol != -1) {
                    checkboxes[currentTopoRow][currentTopoCol].setSelected(false);
                }

                // Generar una nueva posición aleatoria para el topo
                currentTopoRow = ThreadLocalRandom.current().nextInt(0, GRID_SIZE);
                currentTopoCol = ThreadLocalRandom.current().nextInt(0, GRID_SIZE);
                checkboxes[currentTopoRow][currentTopoCol].setSelected(true);

                // Enviar mensaje al servidor con la posición actual del topo
                output.println("TOPO_POSITION " + currentTopoRow + " " + currentTopoCol);

                // Programar la desaparición del topo después de un tiempo determinado
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkboxes[currentTopoRow][currentTopoCol].setSelected(false);
                    }
                }, TOPO_DURATION);
            }
        }, 0, TOPO_REAPPEAR_DELAY);

        // Configurar el evento de clic en los checkboxes
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int finalI = i;
                int finalJ = j;
                checkboxes[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Enviar mensaje al servidor indicando que se ha golpeado al topo
                        output.println("TOPO_HIT " + finalI + " " + finalJ);
                    }
                });
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                System.out.println("Mensaje recibido: " + text);

                String[] parts = text.split(" ");
                String messageType = parts[0];

                if (messageType.equals("TOPIC_HIT")) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    checkboxes[row][col].setSelected(false);
                } else if (messageType.equals("GAME_START")) {
                    System.out.println("¡El juego ha comenzado!");
                } else if (messageType.equals("GAME_END")) {
                    String winnerName = parts[1];
                    if (winnerName.isEmpty()) {
                        System.out.println("El juego ha terminado. No hay ganador.");
                    } else {
                        System.out.println("El juego ha terminado. ¡El ganador es: " + winnerName + "!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
