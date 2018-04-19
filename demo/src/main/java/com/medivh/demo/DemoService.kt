package com.medivh.demo

import android.content.*
import android.os.*
import android.util.Log


import com.medivh.kotlindaemon.AbsWorkService

import java.util.concurrent.*

import io.reactivex.*
import io.reactivex.disposables.*


class DemoService : AbsWorkService() {

    /**
     * 是否 任务完成, 不再需要服务运行?
     * @return 应当停止服务, true; 应当启动服务, false; 无法判断, 什么也不做, null.
     */
    override fun isShouldStopService(intent: Intent, flags: Int, startId: Int): Boolean {
        return sShouldStopService
    }

    override fun startWork(intent: Intent, flags: Int, startId: Int) {
        Log.e(TAG, "检查磁盘中是否有上次销毁时保存的数据")
        sDisposable = Observable
                .interval(3, TimeUnit.SECONDS)
                //取消任务时取消定时唤醒
                .doOnDispose {
                    Log.e(TAG, "保存数据到磁盘。")
                    cancelJobAlarmSub()
                }
                .subscribe { count ->
                    Log.e(TAG, "每 3 秒采集一次数据... count = " + count!!)
                    if (count > 0 && count % 18 == 0L)
                        Log.e(TAG, "保存数据到磁盘。 saveCount = " + (count / 18 - 1))
                }
    }

    override fun stopWork(intent: Intent, flags: Int, startId: Int) {
        stopService()
    }

    /**
     * 任务是否正在运行?
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
    override fun isTaskRunning(intent: Intent, flags: Int, startId: Int): Boolean {
        //若还没有取消订阅, 就说明任务仍在运行.
        return sDisposable != null && !sDisposable!!.isDisposed
    }

    override fun onBind(intent: Intent, v: Void?): IBinder? {
        return null
    }

    override fun onServiceKilled(rootIntent: Intent?) {
        println("保存数据到磁盘。")
    }

    companion object {

        //是否 任务完成, 不再需要服务运行?
        var sShouldStopService: Boolean = false
        var sDisposable: Disposable? = null
        private val TAG = "TAG"

        fun stopService() {
            //我们现在不再需要服务运行了, 将标志位置为 true
            sShouldStopService = true
            //取消对任务的订阅
            if (sDisposable != null) sDisposable!!.dispose()
            //取消 Job / Alarm / Subscription
            cancelJobAlarmSub()
        }
    }
}