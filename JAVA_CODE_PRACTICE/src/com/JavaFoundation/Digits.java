public class Digits {
    public static void main(String[] args) {
        int n1 = 4721;
        int n2 = 7;
        int n3 = 0;
        int n4 = 1000000;
        System.out.println("4721 Digit Count: "+countDigits(n1)+" Sum: "+digitSum(n1));
        System.out.println("4721 Digit Count: "+countDigits(n2)+" Sum: "+digitSum(n2));
        System.out.println("4721 Digit Count: "+countDigits(n3)+" Sum: "+digitSum(n3));
        System.out.println("4721 Digit Count: "+countDigits(n4)+" Sum: "+digitSum(n4));
    }
    static int countDigits(int n){
        int count = 0;
        if(n<10)  return 1;
        while(n>0){
            count++;
            n/=10;
        }
        return count;
    }
    static  int digitSum(int n){
        int sum = 0;
        while(n>0){
            sum = sum + (n%10);
            n/=10;
        }
        return sum;
    }
}
