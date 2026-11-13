package src.com.Phase2.BasicArray;

public class GridSum2DArray {
    public static void main(String[] args) {
        int[][] grid = {
            {3, 8, 1, 9},
            {7, 2, 6, 4},
            {5, 11, 0, 2}
        };
        for (int r = 0; r < grid.length; r++) {
            int sum = 0;
            for (int c = 0; c < grid[r].length; c++) {
                sum += grid[r][c];
            }
            System.out.println("Row " + r + " sum: " + sum);
        }

        for (int c = 0; c < grid[0].length; c++) {
            int sum = 0;
            for (int r = 0; r < grid.length; r++) {
                sum += grid[r][c];
            }
            System.out.println("Col " + c + " sum: " + sum);
        }

        int bestRow = 0, bestCol = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] > grid[bestRow][bestCol]) {
                    bestRow = r;
                    bestCol = c;
                }
            }
        }
        System.out.println("Largest: " + grid[bestRow][bestCol] + " at row " + bestRow + ", col " + bestCol);
    }
}
