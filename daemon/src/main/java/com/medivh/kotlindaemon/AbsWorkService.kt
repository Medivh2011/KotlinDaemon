package com.medivh.kotlindaemon

import android.app.*
import android.content.*
import android.content.pm.*
import android.os.*
import android.support.annotation.*

/**
 * @author medivh
 */
abstract class AbsWorkService : Service() {

    protected var mFirstStarted = true

    /**
     * 是否 任务完成, 不再需要服务运行?
     * @return 应当停止服务, true; 应当启动服务, false; 无法判断, 什么也不做, null.
     */
    abstract fun isShouldStopService(intent: Intent, flags: Int, startId: Int): Boolean

    abstract fun startWork(intent: Intent, flags: Int, startId: Int)
    abstract fun stopWork(intent: Intent, flags: Int, startId: Int)
    /**
     * 任务是否正在运行?
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
    abstract fun isTaskRunning(intent: Intent, flags: Int, startId: Int): Boolean

    abstract fun onBind(intent: Intent, alwaysNull: Void?): IBinder?
    abstract fun onServiceKilled(rootIntent: Intent?)

    /**
     * 1.防止重复启动，可以任意调用 Daemon.startServiceMayBind(Class serviceClass);
     * 2.利用漏洞启动前台服务而不显示通知;
     * 3.在子线程中运行定时任务，处理了运行前检查和销毁时保存的问题;
     * 4.启动守护服务;
     * 5.守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用.
     */
    protected fun onStart(intent: Intent, flags: Int, startId: Int): Int {

        //启动守护服务，运行在:watch子进程中
        Daemon.startServiceMayBind(WatchDogService::class.java!!)

        //业务逻辑: 实际使用时，根据需求，将这里更改为自定义的条件，判定服务应当启动还是停止 (任务是否需要运行)
        val shouldStopService = isShouldStopService(intent, flags, startId)
        if (shouldStopService != null) {
            if (shouldStopService) stopService(intent, flags, startId) else startService(intent, flags, startId)
        }

        if (mFirstStarted) {
            mFirstStarted = false
            //启动前台服务而不显示通知的漏洞已在 API Level 25 修复，大快人心！
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                //利用漏洞在 API Level 17 及以下的 Android 系统中，启动前台服务而不显示通知
                startForeground(HASH_CODE, Notification())
                //利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                    Daemon.startServiceSafely(Intent(application, WorkNotificationService::class.java))
            }
            packageManager.setComponentEnabledSetting(ComponentName(packageName, WatchDogService::class.java!!.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }

        return Service.START_STICKY
    }

    internal fun startService(intent: Intent, flags: Int, startId: Int) {
        //检查服务是否不需要运行
        val shouldStopService = isShouldStopService(intent, flags, startId)
        if (shouldStopService) return
        //若还没有取消订阅，说明任务仍在运行，为防止重复启动，直接 return
        val workRunning = isTaskRunning(intent, flags, startId)
        if (workRunning) return
        //业务逻辑
        startWork(intent, flags, startId)
    }

    /**
     * 停止服务并取消定时唤醒
     *
     * 停止服务使用取消订阅的方式实现，而不是调用 Context.stopService(Intent name)。因为：
     * 1.stopService 会调用 Service.onDestroy()，而 AbsWorkService 做了保活处理，会把 Service 再拉起来；
     * 2.我们希望 AbsWorkService 起到一个类似于控制台的角色，即 AbsWorkService 始终运行 (无论任务是否需要运行)，
     * 而是通过 onStart() 里自定义的条件，来决定服务是否应当启动或停止。
     */
    internal fun stopService(intent: Intent, flags: Int, startId: Int) {
        //取消对任务的订阅
        stopWork(intent, flags, startId)
        //取消 Job / Alarm / Subscription
        cancelJobAlarmSub()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return onStart(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        onStart(intent, 0, 0)
        return onBind(intent, null)
    }

    protected fun onEnd(rootIntent: Intent?) {
        onServiceKilled(rootIntent)
        if (!Daemon.sInitialized) return
        Daemon.startServiceMayBind(Daemon.sServiceClass)
        Daemon.startServiceMayBind(WatchDogService::class.java!!)
    }

    /**
     * 最近任务列表中划掉卡片时回调
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        onEnd(rootIntent)
    }

    /**
     * 设置-正在运行中停止服务时回调
     */
    override fun onDestroy() {
        onEnd(null)
    }

    class WorkNotificationService : Service() {

        /**
         * 利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
         */
        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            startForeground(AbsWorkService.HASH_CODE, Notification())
            stopSelf()
            return Service.START_STICKY
        }

        override fun onBind(intent: Intent): IBinder? {
            return null
        }
    }

    companion object {

        protected val HASH_CODE = 1

        /**
         * 用于在不需要服务运行的时候取消 Job / Alarm / Subscription.
         */
        fun cancelJobAlarmSub() {
            if (!Daemon.sInitialized) return
            Daemon.sApp.sendBroadcast(Intent(WakeUpReceiver.ACTION_CANCEL_JOB_ALARM_SUB))
        }
    }
}