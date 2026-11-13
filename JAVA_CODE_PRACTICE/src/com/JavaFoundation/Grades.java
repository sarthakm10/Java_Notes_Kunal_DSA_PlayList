public class Grades {
    public static void main(String[] args) {
        int marks1 = 90;
        int marks2 = 74;
        int marks3 = 76;

        System.out.println("Grades with IF: "+gradeWithIf(marks1)+" "+gradeWithIf(marks2)+" "+gradeWithIf(marks3));
        System.out.println("Grades with Switch: "+gradeWithSwitch(marks1)+" "+gradeWithSwitch(marks2)+" "+gradeWithSwitch(marks3));

    }
    static String gradeWithIf(int marks){
            if (marks < 0 || marks > 100) {
            return "Invalid";
        } else if (marks >= 90) {
            return "A";
        } else if (marks >= 75) {
            return "B";
        } else if (marks >= 60) {
            return "C";
        } else if (marks >= 40) {
            return "D";
        } else {
            return "F";
        }
    }

    static  String gradeWithSwitch(int marks){
        if (marks < 0 || marks > 100) return "Invalid";
        return switch (marks/10){
            case 10,9 -> "A";
            case 8 -> "B";
            case 7 -> marks>= 75 ? "B" : "C";
            case 6 -> "C";
            case 5,4 -> "D";
            default -> "F";
        };
    }

}
