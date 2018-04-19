package com.medivh.demo

import android.app.Application

import com.medivh.kotlindaemon.Daemon

/**
 * Created by Medivh on 2018/3/23.
 */

class APP : Application() {

    override fun onCreate() {
        super.onCreate()
        initDaemon()

    }

    private fun initDaemon() {
        //需要在 Application 的 onCreate() 中调用一次 DaemonEnv.initialize()
        Daemon.initialize(this, DemoService::class.java, Daemon.DEFAULT_WAKE_UP_INTERVAL)
        DemoService.sShouldStopService = false
        Daemon.startServiceMayBind(DemoService::class.java)
    }
}