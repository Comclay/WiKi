import java.util.Arrays;

public class SortAlgorithm{
	public static void main(String [] args){
		int [] arrs = {3,5,2,1,9,6,8,7,4};
		sort2(arrs);
	}
	
	/*
		—°‘Ò≈≈–Ú
	*/
	public static void sort1(int [] arrays){
		int len = arrays.length;
		for(int i = 0,k = 0; i < len; i++,k=i){
			for(int j = i + 1; j < len; j++){
				if(arrays[j] < arrays[k]){
					k = j;
				}
			}
			if(i != k){
				int temp = arrays[k];
				arrays[k] = arrays[i];
				arrays[i] = temp;
			}
			System.out.println(Arrays.toString(arrays));
		}
	}
	
	/*
		√∞≈›≈≈–Ú
	*/
	public static void sort2(int [] arrays){
		int count = 0;
		for(int i = arrays.length - 1; i > 0; i--){
			count = 0;
			for(int j = 0; j < i; j++){
				if(arrays[j] > arrays[j+1]){
					count++;
					int temp = arrays[j];
					arrays[j] = arrays[j+1];
					arrays[j+1] = temp;
				}
			}
			System.out.println(Arrays.toString(arrays));
			if(count == 0)break;
		}
	}
}