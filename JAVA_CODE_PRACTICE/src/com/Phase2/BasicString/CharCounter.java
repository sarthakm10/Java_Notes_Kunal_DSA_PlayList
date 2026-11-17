package src.com.Phase2.BasicString;

public class CharCounter {
    public static void main(String[] args) {
        String text = "Java 21 is Great!";
        int vowels = 0, consonants = 0, digits = 0, spaces = 0, others = 0;

        String textLc = text.toLowerCase();
        for(int i=0; i<textLc.length(); i++){
            char ch = textLc.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                if (ch == 'a' || ch == 'i' || ch == 'e' || ch == 'u' || ch == 'o' ) {
                    vowels++;
                }else   consonants++;
            }else if (Character.isDigit(ch)) {
                digits++;
            }else if (ch == ' ') {
                spaces++;
            }else   others++;
        }

        System.out.println("Vowels: " + vowels);
        System.out.println("Consonants: " + consonants);
        System.out.println("Digits: " + digits);
        System.out.println("Spaces: " + spaces);
        System.out.println("Others: " + others);
    }
}
