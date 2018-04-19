# Daemon
### Android 服务保活/常驻 (Android Service Daemon)

## 引入

### 1. 添加二进制

```
project build.gradle 中添加
	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
app build.gradle 中添加
    dependencies {
    	        compile 'com.github.Medivh2011:KotlinDaemon:1.1'
    	}
 ```
### 2. 继承 AbsWorkService, 实现 6 个抽象方法

```
/**
 * 是否 任务完成, 不再需要服务运行?
 * @return 应当停止服务, true; 应当启动服务, false; 无法判断, null.
 */
boolean isShouldStopService();

    /**
     * 任务是否正在运行?
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
     fun isTaskRunning(intent: Intent, flags: Int, startId: Int): Boolean

override fun startWork(intent: Intent, flags: Int, startId: Int)

 override fun stopWork(intent: Intent, flags: Int, startId: Int)

//Service.onBind(intent: Intent)
 override fun onBind(intent: Intent, v: Void?): IBinder?

//服务被杀时调用, 可以在这里面保存数据.
 override fun onServiceKilled(rootIntent: Intent?)

```
### 3. 自定义 Application

在 Application 的 `onCreate()` 中, 调用


```
 Daemon.initialize(this, DemoService::class.java, Daemon.DEFAULT_WAKE_UP_INTERVAL)
        DemoService.sShouldStopService = false
        Daemon.startServiceMayBind(DemoService::class.java)
```