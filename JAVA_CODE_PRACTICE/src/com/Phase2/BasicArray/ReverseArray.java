package src.com.Phase2.BasicArray;

import java.util.Arrays;

public class ReverseArray {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5};
        int start = 0;
        int end = arr.length - 1;
        while(start < end){
            swap(arr, start, end);
            start++;
            end--;
        }
        System.out.println(Arrays.toString(arr));
    }
    static void swap(int arr[], int indx1, int indx2){
        int temp = arr[indx1];
        arr[indx1]=arr[indx2];
        arr[indx2]=temp;
    }
}
