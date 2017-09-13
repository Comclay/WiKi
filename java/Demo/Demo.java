public abstract class Demo{

	public static void main(String [] args){

		arrTest();

	}
	
	/*
	*	java中数组的定义有以下三种方式，数组生命的时候不能指定数组大小，且new之后才会分配内存空间和初始化默认值
	*   对象类型初始化的都是null,基本类型初始化的是0，boolean是false,char是" "(空格);
	*/
	public static void arrTest(){
		// 以为数组的三种定义方式
		int [] arr = new int[5];
		arr[0] = 1;
		arr[1] = 2;
		arr[2] = 3;
		arr[3] = 4;
		arr[4] = 5;
		int [] arr2 = new int[]{1,2,3,4,5};
		int [] arr3 = {1,2,3,4,5};
		
		// 二维数组的定义
		int [][] a = {{1,2,3,4},
			{5,6,7,8}};
		System.out.println("数组行数="+a.length);
		System.out.println("数组列数="+a[0].length);
		System.out.println("数组长度="+a.length * a[0].length);
		
		// 测试char类型的初始值
		char [] charArr = new char[2];
		String [] strArr = new String[2];
		System.out.println();
		System.out.println("char类型的数组初始值="+charArr[0]+"a");
		System.out.println("String数组的默认值="+strArr[0]+"a");
	}
	
	public static void change(String str){
		str = "hello world!";
	}

	abstract void test();
	
	public static void testString(){
		String str = "abc";
		change(str);
		System.out.println(str);
	}
	
	public static void testString2(){
		// 字符串常亮都是存放在常量池中，注意：在jdk8中常亮池移到了堆中
		// 栈中值存放了字符串常量的地址，
		String str1 = "hello";
		String str2 = "hello";
		System.out.println(str1 == str2);  // true
		
		
		// 下面这种定义方式：在执行第一行代码的时候会在常量池中存放hello，并且在堆中创建String("hello")
		// 对象，其引用类型str3则存放在栈中；执行第二行代码的时候因为常量池中已经有了所以直接执行后续操作；
		String str3 = new String("hello");
		String str4 = new String("hello");
		System.out.println(str3 == str4);  // false
	}
}