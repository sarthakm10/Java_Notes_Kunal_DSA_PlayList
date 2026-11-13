public class SimpleInterest {
    public static void main(String[] args) {
        double principal = 10000;
        double rate = 8.5;
        int years = 5;

        double interest = principal*rate*years/100;
        // %.2f%n and printf for printing 2 decimal places
        System.out.printf("Interest: %.2f%n",interest);

        double amount = principal+interest;
        System.out.printf("Amount: %.2f%n",amount);
    }
}
