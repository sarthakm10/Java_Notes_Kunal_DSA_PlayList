package src.com.Phase2.BasicArray;

//https://leetcode.com/problems/check-if-array-is-sorted-and-rotated
/* Example 1:

Input:
int[] nums = new int[]{3, 4, 5, 1, 2};

Output: true
Explanation: [1,2,3,4,5] is the original sorted array.
You can rotate the array by x = 2 positions to begin on the element of value 3: [3,4,5,1,2].
 */

public class Leetcode1752 {
    public static void main(String[] args) {
        int[] nums1 = {3, 4, 5, 1, 2};
        System.out.println("Test 1: " + Leetcode1752.check(nums1));

        int[] nums2 = {2, 1, 3, 4};
        System.out.println("Test 2: " + Leetcode1752.check(nums2));

        int[] nums3 = {1, 2, 3};
        System.out.println("Test 3: " + Leetcode1752.check(nums3));
    }
    static boolean check(int[] arr) {
        int trueCount = 0;
        for(int i=0;i<arr.length;i++){
            if(arr[i] > arr[(i+1) % arr.length])  trueCount++;
        }
        return trueCount<=1;
    }

}
