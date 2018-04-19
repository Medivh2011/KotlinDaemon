package com.medivh.kotlindaemon

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.*

import java.util.*

@SuppressLint("StaticFieldLeak")
object Daemon {

    val DEFAULT_WAKE_UP_INTERVAL = 6 * 60 * 1000
    private val MINIMAL_WAKE_UP_INTERVAL = 3 * 60 * 1000

    internal lateinit var sApp: Context
    internal lateinit var sServiceClass: Class<out AbsWorkService>
    private var sWakeUpInterval = DEFAULT_WAKE_UP_INTERVAL
    internal var sInitialized: Boolean = false

    internal val BIND_STATE_MAP: MutableMap<Class<out Service>, ServiceConnection> = HashMap()

    internal val wakeUpInterval: Int
        get() = Math.max(sWakeUpInterval, MINIMAL_WAKE_UP_INTERVAL)

    /**
     * @param app Application Context.
     * @param wakeUpInterval 定时唤醒的时间间隔(ms).
     */
    fun initialize(app: Context, serviceClass: Class<out AbsWorkService>, wakeUpInterval: Int?) {
        sApp = app
        sServiceClass = serviceClass
        if (wakeUpInterval != null) sWakeUpInterval = wakeUpInterval
        sInitialized = true
    }

    fun startServiceMayBind(serviceClass: Class<out Service>) {
        if (!sInitialized) return
        val i = Intent(sApp, serviceClass)
        startServiceSafely(i)
        val bound = BIND_STATE_MAP[serviceClass]
        if (bound == null)
            sApp.bindService(i, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    BIND_STATE_MAP[serviceClass] = this
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    BIND_STATE_MAP.remove(serviceClass)
                    startServiceSafely(i)
                    if (!sInitialized) return
                    sApp.bindService(i, this, Context.BIND_AUTO_CREATE)
                }

                override fun onBindingDied(name: ComponentName) {
                    onServiceDisconnected(name)
                }
            }, Context.BIND_AUTO_CREATE)
    }

    internal fun startServiceSafely(i: Intent) {
        if (!sInitialized) return
        try {
            sApp.startService(i)
        } catch (ignored: Exception) {
        }

    }
}