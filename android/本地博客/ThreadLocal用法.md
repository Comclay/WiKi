&emsp;&emsp;在学习Handler消息机制中Looper源码时看到ThreadLocal这个类，发现它很强大并且很方便的实现了对各个线程中Looper的管理。这个类的源码只有600行。下面先上一个简单的例子：
```java
public class ThreadLocalTest {

	static ThreadLocal<Integer> intLocals = new ThreadLocal<Integer>(){
		protected Integer initialValue() {
	        return 1;
	    }
	};

	public static void main(String[] args) {
		intLocals.set(669);
		new MyThread("A线程").start();
		System.out.println(Thread.currentThread().getName() + "===="
				+ intLocals.get());
	}

	static class MyThread extends Thread {
		public MyThread(String name){
			super(name);
		}
		@Override
		public void run() {
			System.out.println(Thread.currentThread().getName() + "===="
					+ intLocals.get());
		}
	}
}
```
这段代码的运行结果：
```java
main====669
A线程====1
```
**提出问题**：用static关键字修饰的静态变量`intLocals`没效果吗，A线程中的输出结果不应该是669吗？
下面带着这个疑问去源码中寻找答案：
ThreadLocal类中只有一个空参的构造方法，所以关键的代码只有`initLocals.set(669)`和`initLocals.get()`了。
set方法源码：
```java
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * @param  t the current thread
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }
```
从源码中可以看出在Thread中定义了一个ThreadLocalMap的引用，如果该引用的对象不为null就会通过ThreadLocalMap的引用来调用set方法。ThreadLocalMap是ThreadLocal的一个静态内部类，而在ThreadLocalMap中还定义的一个静态的Entry类。以ThreadLocal<?>作为键、Object为值的简直对数据结构；并将键存放到了WeakReference中，即线程中没有ThreadLocal的其他引用时就会自动回收。如果map为空就会执行createMap()方法创建一个ThreadLocalMap对象。
```java
    /**
     * The entries in this hash map extend WeakReference, using
     * its main ref field as the key (which is always a
     * ThreadLocal object).  Note that null keys (i.e. entry.get()
     * == null) mean that the key is no longer referenced, so the
     * entry can be expunged from table.  Such entries are referred to
     * as "stale entries" in the code that follows.
     */
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }

	// 默认的数组大小为16，自定义的话必须为2的幂
    private static final int INITIAL_CAPACITY = 16;

    private Entry[] table;
```
由此可见`map.set(this, value)`只是将当前的intLocals对象作为键，669作为值存储到主线程中。
get方法源码：
```java
    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```
get()通过获取当前线程的ThreadLocalMap对象map，遍历map中数组拿到键为this(即当前ThreadLocal的引用)的Entry实体，从而返回对应的键；map为空时就返回初始化时的值。
&emsp;&emsp;至此，应该能够解答示例中的疑问：ThreadLocal中set(value)方法将调用这个方法的对象作为键、value为值存储到当前线程中ThreadLocalMap中；而get()方法取出时是根据调用这个方法的ThreadLocal对象到当前线程的ThreadLocalMap中查找对应的值，为空时就返回初始值。也就是说ThreadLocal的get和set方法的操作对象其实都是执行这两个方法所在线程的ThreadLocalMap对象，ThreadLocal只起到了一个键的作用。
&emsp;&emsp;ThreadLocal将对象的访问范围限制在线程中，并且当线程结束后ThreadLocal会被自动回收，也可以调用remove()（since 1.5）方法去掉线程中保存的变量。
&emsp;&emsp;另外，ThreadLocal不是为了解决线程间的同步问题，感觉恰恰相反，它是为了避免产生同步问题。既然如此，为什么又要整出ThreadLocal这么个东西，直接搞个局部变量不就好了吗？