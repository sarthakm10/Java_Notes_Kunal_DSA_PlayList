public class Calculator {
    public static void main(String[] args) {
        System.out.println("8 + 2 = " + calculate(8, '+', 2));
        System.out.println("8 - 2 = " + calculate(8, '-', 2));
        System.out.println("8 * 2 = " + calculate(8, '*', 2));
        System.out.println("7 / 2 = " + calculate(8, '/', 2));
        System.out.println("7 / 2 = " + calculate(7, '/', 2));
        System.out.println("8 / 0 = " + calculate(8, '/', 0));
        System.out.println("8 % 2 = " + calculate(8, '%', 2));
    }
    static String calculate(double a, char op, double b){
        if(op=='/' && b == 0)   return "Cannot divide by zero";
        return switch(op){
            case '+' -> String.valueOf(a+b);
            case '-' -> String.valueOf(a-b);
            case '/' -> String.valueOf(a/b);
            case '*' -> String.valueOf(a*b);
            default -> "Unknown operator: " + op;
        };
    }
}
