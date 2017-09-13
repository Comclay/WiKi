
/*

多线程编程情况下存在三个问题：
	1，原子性
	2，可见性
	3，有序性：在不影响程序顺序执行结果的情况下，编译器会对指令进行重排序

volatile关键字的作用是为了解决缓存一致性问题：因为计算机指令的执行速度远远快于数据的读取操作，所有在cpu和内存之间引入了
高速缓存；如果实在多线环境下，线程T1和T2分别从内存中读取了数据x并存储到高速缓存里对应的各自工作区中，当线程T1对数据修改并存储到
高速缓存里，但没有及时写到主存中，此时线程T2所对应的值是修改之前的旧值就会导致结果的错误；

而volatile关键字修饰的变量能保证可见性，即每次修改之后都立即刷新到主存中，保证其他线程获取的值是最新的值；
能保证一定的有序性；

但是还有一个问题：
	volatile关键字不保证操作的原子性；
	
解决方法：
	1，synchronized
	2，lock(),unlock();wait(),notify(),notifyAll()锁机制
		java.util.concurrent.locks.ReentrantLock
	3，java.util.concurrent.atomic.AtomicInteger，这个api里面封装了一些数据的自增，自减，加减操作并具有原子性
		atomic是利用CAS来实现原子性操作的（Compare And Swap），CAS实际上是利用处理器提供的CMPXCHG指令实现的
		，而处理器执行CMPXCHG指令是一个原子性操作。


*/
public class VolatileDemo{
	
	public volatile int a = 0;
	
	public void increase(){
		a++;
	}
	
	public static void main(String [] args){
		final VolatileDemo demo = new VolatileDemo();
		for(int i = 0; i<10;i++){
			new Thread(){
				public void run(){
					for(int j = 0;j<1000;j++){
						demo.increase();
					}
				}
			}.start();
		}
		
		while(Thread.activeCount() > 1){
			Thread.yield();
		}
		
		System.out.println(demo.a);
	}
}