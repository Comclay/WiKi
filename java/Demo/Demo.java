public abstract class Demo{

	public static void main(String [] args){

		arrTest();

	}
	
	/*
	*	java������Ķ������������ַ�ʽ������������ʱ����ָ�������С����new֮��Ż�����ڴ�ռ�ͳ�ʼ��Ĭ��ֵ
	*   �������ͳ�ʼ���Ķ���null,�������ͳ�ʼ������0��boolean��false,char��" "(�ո�);
	*/
	public static void arrTest(){
		// ��Ϊ��������ֶ��巽ʽ
		int [] arr = new int[5];
		arr[0] = 1;
		arr[1] = 2;
		arr[2] = 3;
		arr[3] = 4;
		arr[4] = 5;
		int [] arr2 = new int[]{1,2,3,4,5};
		int [] arr3 = {1,2,3,4,5};
		
		// ��ά����Ķ���
		int [][] a = {{1,2,3,4},
			{5,6,7,8}};
		System.out.println("��������="+a.length);
		System.out.println("��������="+a[0].length);
		System.out.println("���鳤��="+a.length * a[0].length);
		
		// ����char���͵ĳ�ʼֵ
		char [] charArr = new char[2];
		String [] strArr = new String[2];
		System.out.println();
		System.out.println("char���͵������ʼֵ="+charArr[0]+"a");
		System.out.println("String�����Ĭ��ֵ="+strArr[0]+"a");
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
		// �ַ����������Ǵ���ڳ������У�ע�⣺��jdk8�г������Ƶ��˶���
		// ջ��ֵ������ַ��������ĵ�ַ��
		String str1 = "hello";
		String str2 = "hello";
		System.out.println(str1 == str2);  // true
		
		
		// �������ֶ��巽ʽ����ִ�е�һ�д����ʱ����ڳ������д��hello�������ڶ��д���String("hello")
		// ��������������str3������ջ�У�ִ�еڶ��д����ʱ����Ϊ���������Ѿ���������ֱ��ִ�к���������
		String str3 = new String("hello");
		String str4 = new String("hello");
		System.out.println(str3 == str4);  // false
	}
}