package com.example.rootscriptexecutor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private val scriptList = mutableListOf<String>()
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById(R.id.console_output)
        consoleScroll = findViewById(R.id.console_scroll)
        val listView = findViewById<ListView>(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
        listView.adapter = adapter

        // 点击刷新
        btnRefresh.setOnClickListener { 
            Toast.makeText(this, "正在请求 Root 权限并读取目录...", Toast.LENGTH_SHORT).show()
            refreshScripts() 
        }

        // 点击执行
        listView.setOnItemClickListener { _, _, position, _ ->
            val scriptName = scriptList[position]
            executeWithTerminal(scriptName)
        }

        // 启动时自动刷新
        refreshScripts()
    }

    private fun refreshScripts() {
        // 关键修复：把读取文件的操作放到后台线程，防止主线程卡死或权限申请失败
        Thread {
            scriptList.clear()
            // 确保以 Root 身份执行 ls
            val result = Shell.cmd("ls /data/adb/5/ | grep .sh").exec()
            
            runOnUiThread {
                if (result.isSuccess) {
                    scriptList.addAll(result.out)
                    if (scriptList.isEmpty()) {
                        consoleOutput.text = ">>> 提示: /data/adb/5/ 目录下没有 .sh 文件\n"
                    }
                } else {
                    consoleOutput.text = ">>> 错误: 无法读取目录，请确认已授予 Root 权限！\n"
                }
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun executeWithTerminal(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 正在启动脚本: $name\n--------------------\n"

        Thread {
            val outputLines = Collections.synchronizedList(mutableListOf<String>())
            // 同时捕获标准输出和错误输出
            val result = Shell.cmd("sh $fullPath").to(outputLines, outputLines).exec()

            runOnUiThread {
                val finalContent = StringBuilder()
                outputLines.forEach { line ->
                    finalContent.append(line).append("\n")
                }
                
                if (result.isSuccess) {
                    finalContent.append("\n[√] 脚本执行成功")
                } else {
                    finalContent.append("\n[×] 脚本执行失败，返回码: ${result.code}")
                }
                
                consoleOutput.text = finalContent.toString()
                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }.start()
    }
}

