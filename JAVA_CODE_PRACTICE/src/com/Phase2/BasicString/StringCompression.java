package com.Phase2.BasicString;

public class StringCompression {
     public static void main(String[] args) {
        System.out.println(compress("aaabbbbc"));
        System.out.println(compress("aaaaaaaaaab"));
        System.out.println(compress("abc"));
        System.out.println(compress("aabb"));
        System.out.println(compress(""));
    }
    static String compress(String s) {
        StringBuilder result = new StringBuilder();
        int count = 1;  
        for(int i=0; i<s.length(); i++){
            boolean runEnds = i + 1 == s.length() || s.charAt(i) != s.charAt(i + 1);
            if (runEnds) {
                result.append(s.charAt(i)).append(count);
                count = 1;
            } else {
                count++;
            }
        }
        return result.length() < s.length() ? result.toString() : s;
    }
}
