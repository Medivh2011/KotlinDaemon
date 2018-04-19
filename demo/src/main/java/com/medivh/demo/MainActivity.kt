package com.medivh.demo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.medivh.kotlindaemon.IntentWrapper
import com.medivh.kotlindaemon.Daemon



class MainActivity : AppCompatActivity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btn_start -> {
                DemoService.sShouldStopService = false
                Daemon.startServiceMayBind(DemoService::class.java)
            }
            R.id.btn_white -> IntentWrapper.whiteListMatters(this, "轨迹跟踪服务的持续运行")
            R.id.btn_stop -> DemoService.stopService()
        }
    }

    //防止华为机型未加入白名单时按返回键回到桌面再锁屏后几秒钟进程被杀
    override fun onBackPressed() {
        IntentWrapper.onBackPressed(this)
    }
}
