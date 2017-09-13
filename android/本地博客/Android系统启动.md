	首先由Bootloader加载配置引导启动Linux内核，Linux kernal启动之后才会启动Android系统。
> ### 1，启动init进程

在Linux内核启动后，会开启第一个android用户进程Init（\system\core\init\init.cpp），关键代码如下：
```cpp
int main(int argc, char** argv) {
    ......
    if (is_first_stage) {
        boot_clock::time_point start_time = boot_clock::now();
        // Clear the umask.设置创建文件时的默认权限
        umask(0);
        // Get the basic filesystem setup we need put together in the initramdisk
        // on / and then we'll let the rc file figure out the rest.
        mount("tmpfs", "/dev", "tmpfs", MS_NOSUID, "mode=0755");
        mkdir("/dev/pts", 0755);
        mkdir("/dev/socket", 0755);
    
        ......
    }
    // At this point we're in the second stage of init.
    InitKernelLogging(argv);
    LOG(INFO) << "init second stage started!";
    ...... // 一些初始化操作
    ActionManager& am = ActionManager::GetInstance();
    ServiceManager& sm = ServiceManager::GetInstance();
    Parser& parser = Parser::GetInstance();
    parser.AddSectionParser("service", std::make_unique<ServiceParser>(&sm));
    parser.AddSectionParser("on", std::make_unique<ActionParser>(&am));
    parser.AddSectionParser("import", std::make_unique<ImportParser>(&parser));
    std::string bootscript = GetProperty("ro.boot.init_rc", "");
    if (bootscript.empty()) {
        // 解析init.rc文件
        parser.ParseConfig("/init.rc");
        parser.set_is_system_etc_init_loaded(
                parser.ParseConfig("/system/etc/init"));
        parser.set_is_vendor_etc_init_loaded(
                parser.ParseConfig("/vendor/etc/init"));
        parser.set_is_odm_etc_init_loaded(parser.ParseConfig("/odm/etc/init"));
    } else {
        parser.ParseConfig(bootscript);
        parser.set_is_system_etc_init_loaded(true);
        parser.set_is_vendor_etc_init_loaded(true);
        parser.set_is_odm_etc_init_loaded(true);
    }
    ......
    return 0;
}
```
>### 2，启动zygote进程

在init.cpp的main方法中会解析init.rc文件（\system\core\rootdir\init.rc），该文件是通过AIL（Android Init Language）语言定义的初始化文件，具体语法规则在\system\core\init\README.md文件。在init.rc文件中通过下面的语句引入了zygote进程的配置信息。
```cpp
import /init.${ro.zygote}.rc
```
具体的配置信息以init.zygote32.rc（\system\core\rootdir\init.zygote32.rc）文件为例：其中包括zygote的用户、组、优先级、以及重启时的操作信息。
```shell
service zygote /system/bin/app_process -Xzygote /system/bin --zygote --start-system-server
    class main
    priority -20
    user root
    group root readproc
    socket zygote stream 660 root system
    onrestart write /sys/android_power/request_state wake
    onrestart write /sys/power/state on
    onrestart restart audioserver
    onrestart restart cameraserver
    onrestart restart media
    onrestart restart netd
    onrestart restart wificond
    writepid /dev/cpuset/foreground/tasks
```
通过init.zygote32.rc配置信息可以知道zygote的源文件在/system/bin/app_process目录，即app_main.cpp
（源文件\frameworks\base\cmds\app_process\app_main.cpp），下面是该文件的注释，由此可见该文件是应用进程的入口。
```cpp
/*
 * Main entry of app process.
 *
 * Starts the interpreted runtime, then starts up the application.
 *
 */
```
该文件的main方法如下：
```cpp
int main(int argc, char* const argv[])
{
    ......

    // 安卓运行时
    AppRuntime runtime(argv[0], computeArgBlockSize(argc, argv));
    // Process command line arguments
    // ignore argv[0]，第一个参数忽略
    argc--;
    argv++;

    // 配置runtime
    ......
    // 解析runtime的参数，并且忽略掉第一个无效的参数
    bool zygote = false;
    bool startSystemServer = false;
    
    ......
    if (zygote) {
        // 如果zygote进程没有启动就启动zygote
        runtime.start("com.android.internal.os.ZygoteInit", args, zygote);
    } else if (className) {
        runtime.start("com.android.internal.os.RuntimeInit", args, zygote);
    } else {
        fprintf(stderr, "Error: no class name or --zygote supplied.\n");
        app_usage();
        LOG_ALWAYS_FATAL("app_process: no class name or --zygote supplied.");
    }
}
```
app_main.cpp执行完毕之后，正式从c/c++部分转到java部分执行ZygoteInit（\frameworks\base\core\java\com\android\
internal\os\ZygoteInit.java）进程，通过该文件的注释可以了解到该进程中的主要操作有：预加载一些类，等待socket中的一些命令。
```cpp
/**
 * Startup class for the zygote process.
 *
 * Pre-initializes some classes, and then waits for commands on a UNIX domain
 * socket. Based on these commands, forks off child processes that inherit
 * the initial state of the VM.
 *
 */
```
ZygoteInit.java中main方法如下：
```java
public static void main(String argv[]) {
        ZygoteServer zygoteServer = new ZygoteServer();
        // Mark zygote start. This ensures that thread creation will throw
        // an error.
        ZygoteHooks.startZygoteNoThreadCreation();
        // Zygote goes into its own process group.
        // 将进程pid设置为0
        try {
            Os.setpgid(0, 0);
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }
        try {
            // 注册监听
            zygoteServer.registerServerSocket(socketName);
            // 预加载一些关键的类
            preload();

            ......
            // Zygote process unmounts root storage spaces.
            Zygote.nativeUnmountStorageOnInit();
            // Set seccomp policy
            Seccomp.setPolicy();
            ZygoteHooks.stopZygoteNoThreadCreation();
            if (startSystemServer) {
                // 开启SystemServer
                startSystemServer(abiList, socketName, zygoteServer);
            }
            zygoteServer.closeServerSocket();
        } catch (Zygote.MethodAndArgsCaller caller) {
            caller.run();
        } catch (Throwable ex) {
            Log.e(TAG, "System zygote died with exception", ex);
            zygoteServer.closeServerSocket();
            throw ex;
        }
    }
```
其中preload()方法如下：加载android系统及应用运行所必要的资源和类（通过preloadClassess()预加载类文件）
```java
static void preload() {
        Log.d(TAG, "begin preload");
        Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "BeginIcuCachePinning");
        beginIcuCachePinning();
        Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
        Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadClasses");
        preloadClasses();
        Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
        Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadResources");
        preloadResources();
        Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
        Trace.traceBegin(Trace.TRACE_TAG_DALVIK, "PreloadOpenGL");
        preloadOpenGL();
        Trace.traceEnd(Trace.TRACE_TAG_DALVIK);
        preloadSharedLibraries();
        preloadTextResources();
        // Ask the WebViewFactory to do any initialization that must run in the zygote process,
        // for memory sharing purposes.
        WebViewFactory.prepareWebViewInZygote();
        endIcuCachePinning();
        warmUpJcaProviders();
        Log.d(TAG, "end preload");
    }
```
预加载的类信息配置，源文件目录（\frameworks\base\preloaded-classes）
```java
  /**
    * The path of a file that contains classes to preload.
    */
    private static final String PRELOADED_CLASSES = "/system/etc/preloaded-classes";
```
> ### 3，启动SystemServer进程

在ZygoteInit.java的main方法中会调用startSystemServer()来开启SystemServer进程：
```java
/**
     * Prepare the arguments and fork for the system server process.
     */
    private static boolean startSystemServer(String abiList, String socketName, ZygoteServer zygoteServer)
            throws Zygote.MethodAndArgsCaller, RuntimeException {
.......   
        /* Hardcoded command line to start the system server */
        // 硬编码的命令行来开启system server
        String args[] = {
            "--setuid=1000",
            "--setgid=1000",
            "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1032,3001,3002,3003,3006,3007,3009,3010",
            "--capabilities=" + capabilities + "," + capabilities,
            "--nice-name=system_server",
            "--runtime-args",
            "com.android.server.SystemServer",
        };
        
        ......

    }
```
SystemServer的源文件目录（\frameworks\base\services\java\com\android\server\SystemServer.java），其中main方法中直接调用了自己的run方法。在run方法中会开启一系列的服务，并调用Looper.prepareMainLooper()和Looper.loop()方法初始化主线程的消息循环。
```java
   /**
     * The main entry point from zygote.
     */
    public static void main(String[] args) {
        new SystemServer().run();
    }
```
```java
private void run() {
        try {
            // Mmmmmm... more memory!
            VMRuntime.getRuntime().clearGrowthLimit();
            // The system server has to run all of the time, so it needs to be
            // as efficient as possible with its memory usage.
            VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);
            // Some devices rely on runtime fingerprint generation, so make sure
            // we've defined it before booting further.
            Build.ensureFingerprintProperty();
            // Within the system server, it is an error to access Environment paths without
            // explicitly specifying a user.
            Environment.setUserRequired(true);
            // Within the system server, any incoming Bundles should be defused
            // to avoid throwing BadParcelableException.
            BaseBundle.setShouldDefuse(true);
            // Ensure binder calls into the system always run at foreground priority.
            BinderInternal.disableBackgroundScheduling(true);
            // Increase the number of binder threads in system_server
            BinderInternal.setMaxThreads(sMaxBinderThreads);
            // Prepare the main looper thread (this thread).
            android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_FOREGROUND);
            android.os.Process.setCanSelfBackground(false);
            // 此处调用Looper.prepareMainLooper()方法初始化主线程中Looper
            Looper.prepareMainLooper();
            // Initialize native services.
            System.loadLibrary("android_servers");
            // Check whether we failed to shut down last time we tried.
            // This call may not return.
            performPendingShutdown();
            // Initialize the system context.
            createSystemContext();
            // Create the system service manager.
            mSystemServiceManager = new SystemServiceManager(mSystemContext);
            mSystemServiceManager.setRuntimeRestarted(mRuntimeRestart);
            LocalServices.addService(SystemServiceManager.class, mSystemServiceManager);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
        }
        // Start services.
        try {
            Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "StartServices");
            // 开启辅助服务，包括传感器，亮度
            startBootstrapServices();
            // 开启核心服务，BatteryService，UsageStatsService，WebViewUpdateService
            startCoreServices();
            // 开启一些其它的服务：VibratorService，NetworkManagementService，ConnectivityService，NetworkStatsService...
            startOtherServices();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting system services", ex);
            throw ex;
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
        }
        // Loop forever.
        Looper.loop();

    }
```

