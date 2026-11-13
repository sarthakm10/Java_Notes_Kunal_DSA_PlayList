public class LeapYear {
    public static void main(String[] args) {
        int year1 = 2000;
        int year2 = 2002;
        System.out.println("2000: "+isLeap(year1));
        System.out.println("2002: "+isLeap(year2));
    }
    static boolean isLeap(int year){
        if((year % 4 == 0 && year % 100 == 0) || year % 400 == 0 ){
            return true;
        }else   return false;
    }
}
