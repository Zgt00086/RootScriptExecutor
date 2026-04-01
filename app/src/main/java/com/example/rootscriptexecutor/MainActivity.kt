package com.example.rootscriptexecutor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动后台 Root 服务
        startService(Intent(this, RootService::class.java))
        // 启动完立刻关闭界面，实现“隐身”效果
        finish()
    }
}

