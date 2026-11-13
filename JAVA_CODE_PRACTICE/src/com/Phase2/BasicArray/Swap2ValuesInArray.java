package src.com.Phase2.BasicArray;

import java.util.Arrays;

public class Swap2ValuesInArray {
    public static void main(String[] args) {
        int[] arr = {1,3,5,9,19};
        swap(arr,0,4);
        System.out.println(Arrays.toString(arr));
    }
    static void swap(int[] arr, int indx1, int indx2){
        int temp = arr[indx1];
        arr[indx1] = arr[indx2];
        arr[indx2] = temp;
    }
}
