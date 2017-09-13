
import java.util.*;

public class ThreadDemo{
	
	public List names = new ArrayList();
	
	public synchronized void add(String name){
		names.add(name);
	}
	
	public synchronized void printAll(){
		for(int i = 0; i<names.size();i++){
			print(names.get(i)+"");
		}
	}
	
	public static void main(String [] args){
		test1();
	}

	public static void test1(){
		final ThreadDemo name = new ThreadDemo();
		for(int i =0;i<2;i++){
			new Thread(){
				public void run(){
					name.add("A");
					name.add("B");
					name.add("C");
					
					name.printAll();
				}
			}.start();
		}
	}

	public static void print(String msg){
		System.out.print(msg);
	}
	
	public static void println(String msg){
		System.out.println(msg);
	}
}