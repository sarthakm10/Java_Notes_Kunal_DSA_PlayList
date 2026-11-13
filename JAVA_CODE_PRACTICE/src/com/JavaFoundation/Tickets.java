public class Tickets {
    public static void main(String[] args) {
        System.out.println(ticketPrice(2, true, false));
        System.out.println(ticketPrice(8, false, false));
        System.out.println(ticketPrice(8, true, true));
        System.out.println(ticketPrice(20, false, true));
        System.out.println(ticketPrice(20, true, true));
        System.out.println(ticketPrice(30, false, false));
        System.out.println(ticketPrice(65, true, false));
    }
    static int ticketPrice(int age, boolean isWeekend, boolean isStudent){
        if(age<=3){
            return 0;
        }
        int basePrice = isWeekend ? 250:200;
        if(age<=12 || age>=60)  return basePrice/2;
        else if(isStudent && age >= 13 && age <= 25 && !isWeekend)  return basePrice-50;
        else return basePrice;
    }
}
