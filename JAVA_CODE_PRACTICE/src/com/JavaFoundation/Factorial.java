public class Factorial {
    public static void main(String[] args) {
        int n1 = 0;
        int n2 = 5;
        int n3 = 20;
        System.out.println("0 Fact: "+factorial(n1));
        System.out.println("5 Fact: "+factorial(n2));
        System.out.println("20 Fact: "+factorial(n3));
    }
    static long factorial(int n){
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
