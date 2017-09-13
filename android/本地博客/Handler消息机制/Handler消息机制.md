&emsp;&emsp;Handler经常被我们用来在主线程和子线程之间传递消息，因此这里打算从源码的角度分析下它的使用，也算是进军源码的一个开端！为了使整个分析过程比较的有条理，便从一个最简单的使用场景（代码如下：）着手，顺着源码来看Message的传递流程。
```java
public class MainActivity extends AppCompatActivity {

    public static final String MSG = "msg";
	// 注意内存泄露的问题
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, msg.getData().getString(MSG), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                Message msg = Message.obtain();
                Bundle bundle= new Bundle();
                bundle.putString(MSG,"子线程发送的消息！");
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }).start();
    }
}
```
Handler中的消息处理机制主要包含四个类：
1. Message消息对象
2. Handler消息处理器
3. MessageQueue消息队列
4. Looper消息轮询器

下面是用这四个类画的时序图：7-12步是在子线程中执行的，其他在主线中执行。
![](http://i.imgur.com/zdG9BiZ.png)
一、消息机制的初始化
------
&emsp;&emsp;在启动UI主线程ActivityThread（\frameworks\base\core\java\android\app\ActivityThread.java）会执行下面的main方法：
```java
public static void main(String[] args) {
	Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");

	// CloseGuard defaults to true and can be quite spammy.  We
	// disable it here, but selectively enable it later (via
	// StrictMode) on debug builds, but using DropBox, not logs.
	CloseGuard.setEnabled(false);

	Environment.initForCurrentUser();

	// Set the reporter for event logging in libcore
	EventLogger.setReporter(new EventLoggingReporter());

	// Make sure TrustedCertificateStore looks in the right place for CA certificates
	final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
	TrustedCertificateStore.setDefaultUserDirectory(configDir);

	Process.setArgV0("<pre-initialized>");

	Looper.prepareMainLooper();

	ActivityThread thread = new ActivityThread();
	thread.attach(false);

	if (sMainThreadHandler == null) {
		// 创建H extends Handler对象，H类中定义了许多跟Activity生命周期相关的常量。
		sMainThreadHandler = thread.getHandler();
	}

	if (false) {
		Looper.myLooper().setMessageLogging(new
						LogPrinter(Log.DEBUG, "ActivityThread"));
	}

	// End of event ActivityThreadMain.
	Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
	Looper.loop();

	throw new RuntimeException("Main thread loop unexpectedly exited");
}
```
## 1，Looper.prepareMainLooper() ##
在main方法中调用了`Looper.prepareMainLooper()`，从下面的源码中可以看出最终调用的还是`prepare()`方法，并且用ThreadLocal保存了Looper的对象，这样能够保证每个Looper对象都与线程对应，并且从抛出的异常信息中可以知道消息机制的一个重要特点：**每个线程中只能创建一个Looper对象**。
```java
public static void prepare() {
	prepare(true);
}

private static void prepare(boolean quitAllowed) {
	if (sThreadLocal.get() != null) {
		throw new RuntimeException("Only one Looper may be created per thread");
	}
	sThreadLocal.set(new Looper(quitAllowed));
}

public static void prepareMainLooper() {
	prepare(false);
	synchronized (Looper.class) {
		if (sMainLooper != null) {
			throw new IllegalStateException("The main Looper has already been prepared.");
		}
		sMainLooper = myLooper();
	}
}
```
Looper的构造方法中创建了MessageQueue的实例
```java
private Looper(boolean quitAllowed) {
    mQueue = new MessageQueue(quitAllowed);
    mThread = Thread.currentThread();
}
```
**Looper.prepare()方法实例化了Looper和MessageQueue对象。**
## 2，Looper.loop() ##
在main方法的最后执行Looper.loop()进入死循环，其中`queue.next()`是一个阻塞式方法，直到消息队列中获取到Message对象才会执行后面的`dispatchMessage(msg)`来处理消息。
```java
	/**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;
        ......

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
			......
            try {
                msg.target.dispatchMessage(msg);
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
            ......
			// 将msg回收到对象池中，便于下次复用
            msg.recycleUnchecked();
        }
    }
```
二、Handler的创建
---------
Handler的创建除了示例中的匿名内部类方式，还可以通过实现Callback的方式来创建。这两种方法本质上是一样的，只不过一提到接口就想到多继承扩展啥的。代码的日志就是经常在代码中看到的警告：应该使用静态的内部类或者可能出现内存泄露。
```java
    public Handler() {
        this(null, false);
    }

    public Handler(Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }

        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```

三、Message的创建
----------
## 1，Message的数据结构 ##
先看Message类的定义，Message中除了包括传递数据所用的变量what、data、when外，还定义了一个next的引用，这说明Message是一个链表的数据结构，而MessageQueue虽说叫一个消息队列，实际上是个链表。另外，Messge中为了避免重复的创建Message对象造成内存的抖动，也提供了一个复用的对象池，最多容纳50个对象。
```java
public final class Message implements Parcelable {
    public int what;
	......
    /*package*/ long when;
    /*package*/ Bundle data;
    /*package*/ Handler target;
    /*package*/ Runnable callback;
    
    // sometimes we store linked lists of these things
    /*package*/ Message next;
    private static Message sPool;
    private static int sPoolSize = 0;

    private static final int MAX_POOL_SIZE = 50;
	......
}
```
## 2，Message.obtain() ##
该方法中会先判断对象池中是否有可复用的Message对象，如果有就从链表头部取出一个Message对象返回，否则就会重新创建一个Message对象。除了这个无参的方法，还有7个重载方法，这些重载方法无非就是加了一些初始化参数并且都调用了`obtain()`方法。
```java
    /**
     * Return a new Message instance from the global pool. Allows us to
     * avoid allocating new objects in many cases.
     */
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.flags = 0; // clear in-use flag
                sPoolSize--;
                return m;
            }
        }
        return new Message();
    }
```
四、Message的发送
---------------
## 1，sendMessage() ##
消息创建完后就可以通过handler将消息发送出去，主要有sendXxx(),sendEmptyXxx(),postXxx()三类方法，sendXxx方法是在调用之前就初始化好了Message对象，后面两类方法是在执行过程中封装的Message对象。
- sendXxx()：该系列方法最终调用的都是`sendMessageAtTime(msg,uptimeMills)`。从源代码中可以知道延迟执行的时间不能小于0，并且要求MessageQueue必须先实例化。
```java
	// 发送立即处理的消息
	public final boolean sendMessage(Message msg)
    {
        return sendMessageDelayed(msg, 0);
    }
	// 发送延迟执行的消息
    public final boolean sendMessageDelayed(Message msg, long delayMillis)
    {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
    }
	// 发送在制定时间点执行的消息
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
	// 特殊情况下使用，该方法仅仅是将uptimeMills置为0
    public final boolean sendMessageAtFrontOfQueue(Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
		return enqueueMessage(queue, msg, 0);
    }
```
- sendEmptyXxx()和postXxx()：该系列的方法基本上都有与之对应的sendXxx()方法。
```java
	public final boolean sendEmptyMessage(int what);
	public final boolean sendEmptyMessageDelayed(int what, long delayMillis);
	public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis);
	// Runnable对象会赋值给callback
    public final boolean post(Runnable r);
	public final boolean postAtTime(Runnable r, long uptimeMillis);
    public final boolean postAtTime(Runnable r, Object token, long uptimeMillis);
    public final boolean postDelayed(Runnable r, long delayMillis);
    public final boolean postAtFrontOfQueue(Runnable r);
```
这里以`post(Runnable r)`方法为例，其它的都差不多。msg中的callback指向了该Runnable对象，后面在处理的时候会直接调用里面的run方法。
```java
    public final boolean post(Runnable r)
    {
       return  sendMessageDelayed(getPostMessage(r), 0);
    }
    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
```
## 2，enqueueMessage(queue, msg, uptimeMillis) ##
从上面的的`sendMessageAtTime()`得知会调用`enqueueMessage()`，源码如下。在这里将当前Handler的引用this赋值给了`msg.target`（其实这是为了避免在创建多个Handler对象的情况下消息的冲突）。
```java
    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
```
MessageQueue中`enqueueMessage()`需要遍历单链表将msg插入到链表中，并且还要判断是否该唤醒主线程，即next()方法。
```java
    boolean enqueueMessage(Message msg, long when) {
   		......
        synchronized (this) {
            .....
            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // 将msg插入到队列中
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
```

五、Message的处理
----------------
## 1，queue.next() ##
当执行`nativeWake(mPtr)`方法唤醒了主线程，MessageQueue的`next()`方法（该方法是在Looper.loop()中调用的，前面有相关源码）就会从链表中取出消息。
```java
    Message next() {
        ......
            nativePollOnce(ptr, nextPollTimeoutMillis);
            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
				......
        }
    }
```
## 2，msg.target.dispatchMessage(msg) ##
在`Looper.loop()`方法中会调用`dispatchMessage()`，具体的源码如下。并且这个方法是在主线程中调用的，因此消息就从子线程传递到了UI线程中啦。
```java
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
```
分为三种情况来处理消息：
- post一个Runnable对象：该方式会执行`handleCallback()`，即执行Runnable中run方法。
```java
    private static void handleCallback(Message message) {
        message.callback.run();
    }
```
- 实现Callback接口：mCallback是在Handler创建的时候初始化的，需要自己去实现这个内部接口。需要注意的是：这个接口中的方法比Handler中的空方法`void handleMessage(Message msg){}`多了个boolean类型的返回值。
```java
    public interface Callback {
        public boolean handleMessage(Message msg);
    }
```
- 发送普通的Message对象：该方式直接调用自己实现的`handleMessage()`方法。当然，这个方法的执行与否还与前面的两种情况有关。

## 3，msg.recycleUnchecked() ##
在msg消息处理完成之后，就会执行`recycleUnchecked()`方法完成的这个msg的复用。该方法会将msg对象中成员变量还原，并采用头插法将msg插入到对象池（Message.sPool）中。
```java
	void recycleUnchecked() {
        // Mark the message as in use while it remains in the recycled object pool.
        // Clear out all other details.
        flags = FLAG_IN_USE;
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        replyTo = null;
        sendingUid = -1;
        when = 0;
        target = null;
        callback = null;
        data = null;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }
```

六、总结
-------
&emsp;&emsp;Handler不仅仅只有上面一种使用场景，还可以用于子线程和子线程，子线程和主线程的通信，只是在使用时要注意以下几点：
1. 在子线程使用时中需调用`Looper.prepare()`方法来初始化Looper和MessageQueue；
2. 在子线程使用时中需调用`Looper.loop()`方法来轮询消息队列；
3. 一个线程中只能有一个Looper对象；
4. 一个线程中可以创建多个Handler对象；
5. 第五部分中消息处理的集中情况（本来想画个流程图但是源码中已经很清晰就懒得画了）

**参考资料**
Android7.0源码