&emsp;&emsp;`AsyncTask`常用来在主线程中执行网络请求等比较耗时的异步任务，但是一直以来自己始终停留在实现一个`AsyncTask`的实例、然后在`doInBackgroud()`方法中执行耗时的任务、最后实现`onPostExcute()`方法更新界面这种层面，从未深入的去了解AsyncTask中的执行过程；对于为什么默认是线性执行的、不能重复执行等特点都没有深究过，虽然也知道，但大多是从别人博客或书中了解到，只知其然而不知其所以然。尽管，AsyncTask已经被写烂了，但那终究是别人的，如果能从AsyncTask的源码中顿悟点什么别的东西，那就是意外收获啦，所以还是很有必要自己再咀嚼一遍。
&emsp;&emsp;AsyncTask是一个抽象类，包含三个泛型参数<Params, Progress, Result>：Params表示传递给工作线程的参数；Progress表示执行异步任务的进度；Result表示执行完异步任务后返回值的类型。在使用时需要定义子类并实现其抽象方法（一般还需要重写onPreExecute、onPostExecute方法）。通过方法声明上的注解可以知道除了doInBackground()是在工作线程（子线程）中执行的，其他三个方法都是在UI线程中调用。
```java
	// 子类必须要实现的方法，用来在工作线程中处理具体的异步任务
    @WorkerThread
    protected abstract Result doInBackground(Params... params);

	// 在doInBackground()之前调用的方法
    @MainThread
    protected void onPreExecute() {
    }

	// 在doInBackground()之后调用的方法
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onPostExecute(Result result) {
    }

	// 用来更新异步任务的执行进度，需要在doInBackground()方法中调用publishProgress()方法
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onProgressUpdate(Progress... values) {
    }
```
&emsp;&emsp;
```java
public abstract class AsyncTask<Params, Progress, Result> {

    public AsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    //noinspection unchecked
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    postResult(result);
                }
                return result;
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }
}
```

&emsp;&emsp;`execute()`是第一个被调用的方法，所以这里就从它来说起。execute有两个重载的方法：

- `execute(Params... params)`可以传递变长的参数列表，返回当前的AsyncTask对象并用final关键字修饰该方法，也就是说不能重写。方法体中是调用的executeOnExecutor()方法，

- `execute(Runnable runnable)`是静态方法并需要传递一个Runnable对象。

```java
    @MainThread
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    @MainThread
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    @MainThread
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        exec.execute(mFuture);

        return this;
    }
```
这两个方法使用的都是`sDefaultExecutor`作为线程池，它是AsyncTask的一个内部类**SerialExecutor**，从下面代码中可以看出`sDefaultExecutor`是通过execute的递归调用达到一个线性执行Runnable任务的目的。最终该任务是在`THREAD_POOL_EXECUTOR`的线程中执行的。
```java
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;

    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
						// 执行完一个Runnable后就可以执行下一个了，保证了线性执行
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }
```
由此可见AsyncTask要执行的一部任务或Runnable都是通过`THREAD_POOL_EXECUTOR`这个线程池来执行的，该线程池本身不保证任务的线性执行，其顺序执行是通过内部类`SerialExecutor`控制。
源码中定义了线程池中核心线程数在2到4之间，具体的要参考根据cpu核数；非核心的线程在执行完任务后存活时间为30s。并且该线程池是在静态代码块中初始化，
```java
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // We want at least 2 threads and at most 4 threads in the core pool,
    // preferring to have 1 less than the CPU count to avoid saturating
    // the CPU with background work
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }
```