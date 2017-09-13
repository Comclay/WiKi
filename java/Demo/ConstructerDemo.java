
class A{
	A(){
		System.out.println("A");
	}
}

/*

默认会调用父类的构造方法
*/
public class ConstructerDemo extends A{


	
	
	public static void main(String [] args){
		new ConstructerDemo();
		
		new A();
	}
}