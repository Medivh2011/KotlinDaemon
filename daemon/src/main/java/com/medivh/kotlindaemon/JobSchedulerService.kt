package com.medivh.kotlindaemon

import android.annotation.*
import android.app.job.*
import android.os.*

/**
 * Android 5.0+ 使用的 JobScheduler.
 * 运行在 :watch 子进程中.
 * @author medivh
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobSchedulerService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        if (!Daemon.sInitialized) return false
        Daemon.startServiceMayBind(Daemon.sServiceClass)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }
}
