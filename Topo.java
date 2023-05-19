import java.util.Random;

public class Topo {
    private int row;
    private int col;

    public Topo(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public static Topo generateRandomTopo(int gridRows, int gridCols) {
        Random random = new Random();
        int row = random.nextInt(gridRows);
        int col = random.nextInt(gridCols);
        return new Topo(row, col);
    }
}
