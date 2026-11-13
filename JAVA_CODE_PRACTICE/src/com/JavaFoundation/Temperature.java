public class Temperature {
    public static void main(String[] args) {
        double tempF = 98.6;
        double C = (tempF - 32) * 5 / 9;
        System.out.println("Temp in C "+ C);
        double tempC = 38;
        double F = tempC * 9/5 + 32;
        System.out.println("Temp in F "+F);
    }
}
