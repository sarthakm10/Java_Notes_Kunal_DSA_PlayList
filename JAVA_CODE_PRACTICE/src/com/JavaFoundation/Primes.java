public class Primes {
    public static void main(String[] args) {
        int n1 = 53;
        int n2 = 54;
        System.out.println("53 is "+isPrime(n1));
        System.out.println("54 is "+isPrime(n2));
    }
    static boolean isPrime(int n){
        if(n<=1)    return false;
        for(int i=2; i*i<=n; i++){
            if(n%i == 0)    return false;
        }
        return true;
    }
}
