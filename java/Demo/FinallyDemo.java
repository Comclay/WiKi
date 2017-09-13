

/*
final:
finally:
finalize;

*/

public class FinallyDemo{
	public int a = 0;
	public static void main(String [] args){
		try{
			println("1");
			return;
			//throw new Exception("抛出异常");
		}catch(Exception e){
			println("2");
		}finally{
			println("3");
		}
		println("4");
	}
	
	public static int testObj(){
		try{
			
		}catch(Exception e){
			
		}finally{
			
		}
	}
	
	public static void println(String msg){
		System.out.println(msg);
	}
}