package src.com.Phase2.BasicString;

//https://leetcode.com/problems/valid-palindrome/description/
/* 
Example 1:
Input:
String s = "A man, a plan, a canal: Panama";
Output: true
Explanation: "amanaplanacanalpanama" is a palindrome.
 */

public class Leeetcode125 {
    public static void main(String[] args) {
        String s = "A man, a plan, a canal: Panama";
        System.out.println(isPalindrome(s));
    }
    static boolean isPalindrome(String s){
        int start = 0;
        int end = s.length() - 1;
        while(start<end){
            char st = s.charAt(start);
            // if(!((st>='a' && st<='z') || (st>='A' && st<='Z') || (st>='0' && st<='9'))){
            if(!Character.isLetterOrDigit(st)){
                start++;
                continue;
            }
            char en = s.charAt(end);
            // if(!((en>='a' && en<='z') || (en>='A' && en<='Z') || (en>='0' && en<='9'))){
            if(!Character.isLetterOrDigit(en)){
                end--;
                continue;
            }
            if(st>='A' && st<='Z')  st=(char) (st+32);
            if(en>='A' && en<='Z')  en=(char) (en+32);
            if(st != en)    return false;
            start++;
            end--;
        }
        return true;
    }
}
