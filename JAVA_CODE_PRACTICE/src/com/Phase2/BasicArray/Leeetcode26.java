package src.com.Phase2.BasicArray;

import java.util.Arrays;

//https://leetcode.com/problems/remove-duplicates-from-sorted-array/description/
/* Example 1:

Input:
int[] nums = new int[]{1, 1, 2};

Output: 2, nums = [1,2,_]
Explanation: Your function should return k = 2, with the first two elements of nums being 1 and 2 respectively.
It does not matter what you leave beyond the returned k (hence they are underscores).
 */

public class Leeetcode26 {
    public static void main(String[] args) {
        int[] nums = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int k = Leeetcode26.removeDuplicates(nums);
        System.out.println("k = " + k);
        System.out.println("Modified array = " + Arrays.toString(Arrays.copyOf(nums, k)));
    }
    static int removeDuplicates(int[] nums){
        int k = 1;
        for(int i = 1; i < nums.length; i++){
            if(nums[i] != nums[i-1]){
                nums[k]=nums[i];
                k++;
            }
        }
        return k;
    }
}
