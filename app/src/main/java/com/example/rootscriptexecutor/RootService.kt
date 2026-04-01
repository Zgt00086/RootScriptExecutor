package com.example.rootscriptexecutor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.Shell

class RootService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在后台线程执行 Root 指令，避免卡死应用
        Thread {
            try {
                // 检查 Root 权限并执行指令
                if (Shell.getShell().isRoot) {
                    Log.d("RootExecutor", "Root 权限获取成功，准备执行脚本")
                    
                    // 这里填写你真正想执行的命令或脚本路径
                    // 例如：Shell.cmd("sh /sdcard/myscript.sh").exec()
                    Shell.cmd("echo 'Root Executor is running' > /data/local/tmp/root_test.log").exec()
                    
                } else {
                    Log.e("RootExecutor", "未能获取 Root 权限，请检查 Magisk/KernelSU 授权")
                }
            } catch (e: Exception) {
                Log.e("RootExecutor", "执行出错: ${e.message}")
            } finally {
                // 执行完任务后，自动停止服务，不占用后台内存
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }
}

