package com.medivh.kotlindaemon

import android.content.*

/**
 * @author medivh
 */
class WakeUpReceiver : BroadcastReceiver() {

    /**
     * 监听 8 种系统广播 :
     * CONNECTIVITY\_CHANGE, USER\_PRESENT, ACTION\_POWER\_CONNECTED, ACTION\_POWER\_DISCONNECTED,
     * BOOT\_COMPLETED, MEDIA\_MOUNTED, PACKAGE\_ADDED, PACKAGE\_REMOVED.
     * 在网络连接改变, 用户屏幕解锁, 电源连接 / 断开, 系统启动完成, 挂载 SD 卡, 安装 / 卸载软件包时拉起 Service.
     * Service 内部做了判断，若 Service 已在运行，不会重复启动.
     * 运行在:watch子进程中.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && ACTION_CANCEL_JOB_ALARM_SUB == intent.action) {
            WatchDogService.cancelJobAlarmSub()
            return
        }
        if (!Daemon.sInitialized) return
        Daemon.startServiceMayBind(Daemon.sServiceClass)
    }

    class WakeUpAutoStartReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (!Daemon.sInitialized) return
            Daemon.startServiceMayBind(Daemon.sServiceClass)
        }
    }

    companion object {

        /**
         * 向 WakeUpReceiver 发送带有此 Action 的广播, 即可在不需要服务运行的时候取消 Job / Alarm / Subscription.
         */
        val ACTION_CANCEL_JOB_ALARM_SUB = "com.medivh.damon.CANCEL_JOB_ALARM_SUB"
    }
}