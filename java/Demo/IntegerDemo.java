public class IntegerDemo{
	
	
	public static void main(String [] args){
		Integer a = 59;
		int b = 59;
		Integer c = Integer.valueOf(59);
		Integer d = new Integer(59);
		
		System.out.println(a==b);
		System.out.println(a==c);
		System.out.println(c==d);
		System.out.println(b==d);
	}
}