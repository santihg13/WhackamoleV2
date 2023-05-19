import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Juego {
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 5;
    private static final int SCORE_THRESHOLD = 5;

    private static InterfazGrafica interfaz;

    public static void main(String[] args) {
        Servidor servidor = new Servidor(GRID_ROWS, GRID_COLS, SCORE_THRESHOLD);
        servidor.iniciar();

        interfaz = new InterfazGrafica();
        interfaz.setVisible(true);

        RegistroServidor registroServidor = new RegistroServidor(servidor, interfaz);
        registroServidor.start();
    }
}
