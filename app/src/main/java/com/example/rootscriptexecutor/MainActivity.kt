package com.example.rootscriptexecutor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private val scriptList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView = findViewById<ListView>(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
        listView.adapter = adapter

        // 刷新列表
        btnRefresh.setOnClickListener { refreshScripts() }

        // 点击执行脚本
        listView.setOnItemClickListener { _, _, position, _ ->
            val scriptName = scriptList[position]
            executeScript(scriptName)
        }

        refreshScripts() // 启动时自动刷新一次
    }

    private fun refreshScripts() {
        scriptList.clear()
        // 修正点：使用 exec().out 来获取命令输出的行列表
        val result = Shell.cmd("ls /data/adb/5/ | grep .sh").exec()
        scriptList.addAll(result.out)
        
        if (scriptList.isEmpty()) {
            Toast.makeText(this, "目录下未找到脚本文件", Toast.LENGTH_SHORT).show()
        }
        adapter.notifyDataSetChanged()
    }

    private fun executeScript(name: String) {
        val fullPath = "/data/adb/5/$name"
        Toast.makeText(this, "正在执行: $name", Toast.LENGTH_SHORT).show()
        
        Thread {
            // 执行脚本，并在主线程弹出提示
            Shell.cmd("sh $fullPath").exec()
            runOnUiThread {
                Toast.makeText(this, "$name 指令已发送", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}

