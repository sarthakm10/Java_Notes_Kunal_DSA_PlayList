package src.com.Phase2.BasicArray;

public class MaxMinAvg2ndMax {
    public static void main(String[] args) {
        int[] arr = { 72, 95, 58, 81, 64, 90, 47 };
        int max = Integer.MIN_VALUE;
        int max2nd = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int total = 0;

        for (int n : arr) {
            if (n > max) {
                max2nd = max;
                max = n;
            } else if (n > max2nd && n != max) {
                max2nd = n;
            }
            if (n < min)
                min = n;
            total += n;
        }
        double avg = (double) total / arr.length;

        System.out.println("Max " + max + " 2nd Max: " + max2nd + " Min: " + min + " Avg: " + avg);
    }
}
