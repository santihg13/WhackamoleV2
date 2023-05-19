import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterfazGrafica extends JFrame {
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 5;

    private JButton[][] buttons;

    public InterfazGrafica() {
        setTitle("Whack-a-Mole");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(GRID_ROWS, GRID_COLS));
        buttons = new JButton[GRID_ROWS][GRID_COLS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(80, 80));
                button.setEnabled(false);
                buttons[row][col] = button;
                mainPanel.add(button);

                final int finalRow = row;
                final int finalCol = col;
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Cliente.enviarGolpe(finalRow, finalCol);
                    }
                });
            }
        }

        add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public void updateButton(int row, int col, boolean hasMole) {
        buttons[row][col].setEnabled(hasMole);
    }
}
