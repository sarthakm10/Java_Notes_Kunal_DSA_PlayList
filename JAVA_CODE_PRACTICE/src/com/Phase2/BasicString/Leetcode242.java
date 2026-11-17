package src.com.Phase2.BasicString;

//https://leetcode.com/problems/valid-anagram/
/* 
Given two strings s and t, return true if t is an anagram of s, and false otherwise.
Example 1:
Input:
String s = "anagram";
String t = "nagaram";
Output: true

Example 2:
Input:
String s = "rat";
String t = "car";
Output: false
 */

public class Leetcode242 {
    public static void main(String[] args) {
        String s = "anagram";
        String t = "nagaram";
        System.out.println(isAnagram(s, t));
    }
    static boolean isAnagram(String s, String t){
        if(s.length()!=t.length())  return false;
        int[] count = new int[26];
        for(char c:s.toCharArray()){
            count[c-'a']++;
        }
        for(char c:t.toCharArray()){
            count[c-'a']--;
        }
        for(int c : count ) if(c != 0)  return false;
        return true;
    }
}
