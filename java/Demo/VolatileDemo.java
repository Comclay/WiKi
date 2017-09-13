
/*

���̱߳������´����������⣺
	1��ԭ����
	2���ɼ���
	3�������ԣ��ڲ�Ӱ�����˳��ִ�н��������£����������ָ�����������

volatile�ؼ��ֵ�������Ϊ�˽������һ�������⣺��Ϊ�����ָ���ִ���ٶ�ԶԶ�������ݵĶ�ȡ������������cpu���ڴ�֮��������
���ٻ��棻���ʵ�ڶ��߻����£��߳�T1��T2�ֱ���ڴ��ж�ȡ������x���洢�����ٻ������Ӧ�ĸ��Թ������У����߳�T1�������޸Ĳ��洢��
���ٻ������û�м�ʱд�������У���ʱ�߳�T2����Ӧ��ֵ���޸�֮ǰ�ľ�ֵ�ͻᵼ�½���Ĵ���

��volatile�ؼ������εı����ܱ�֤�ɼ��ԣ���ÿ���޸�֮������ˢ�µ������У���֤�����̻߳�ȡ��ֵ�����µ�ֵ��
�ܱ�֤һ���������ԣ�

���ǻ���һ�����⣺
	volatile�ؼ��ֲ���֤������ԭ���ԣ�
	
���������
	1��synchronized
	2��lock(),unlock();wait(),notify(),notifyAll()������
		java.util.concurrent.locks.ReentrantLock
	3��java.util.concurrent.atomic.AtomicInteger�����api�����װ��һЩ���ݵ��������Լ����Ӽ�����������ԭ����
		atomic������CAS��ʵ��ԭ���Բ����ģ�Compare And Swap����CASʵ���������ô������ṩ��CMPXCHGָ��ʵ�ֵ�
		����������ִ��CMPXCHGָ����һ��ԭ���Բ�����


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