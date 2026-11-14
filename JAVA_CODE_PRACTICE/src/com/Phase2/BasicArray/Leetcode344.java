package src.com.Phase2.BasicArray;

import java.util.Arrays;

//https://leetcode.com/problems/reverse-string/description/
/* Example 1:
Input:
char[] s = new char[]{'h', 'e', 'l', 'l', 'o'};
Output: ["o","l","l","e","h"] 
*/

public class Leetcode344 {
    public static void main(String[] args) {
        char[] s = new char[]{'h', 'e', 'l', 'l', 'o'};
        reverseString(s);
        System.out.println(Arrays.toString(s));
    }
    static void reverseString(char[] s){
        int start = 0;
        int end = s.length - 1;
        while(start < end){
            char temp = s[start];
            s[start++] = s[end];
            s[end--] = temp;
        }
    }
}
