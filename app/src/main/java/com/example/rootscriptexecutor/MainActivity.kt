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
        val listView = findViewById(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
        listView.adapter = adapter

        btnRefresh.setOnClickListener { refreshScripts() }
        listView.setOnItemClickListener { _, _, position, _ ->
            executeWithTerminal(scriptList[position])
        }

        // 启动时直接刷，不等了
        refreshScripts()
    }

    private fun refreshScripts() {
        Thread {
            try {
                // 强制唤醒全局 Root Shell
                val isRoot = Shell.getShell().isRoot
                
                // 直接跑 ls，不做任何过滤逻辑
                val output = Shell.cmd("ls /data/adb/5/").exec().out
                
                // 拿到结果后再在 UI 上过滤 .sh
                val filtered = output.filter { it.endsWith(".sh") }

                runOnUiThread {
                    scriptList.clear()
                    scriptList.addAll(filtered)
                    adapter.notifyDataSetChanged()
                    
                    if (!isRoot) {
                        consoleOutput.append(">>> [警告] 未检测到 Root 权限！\n")
                    } else if (filtered.isEmpty()) {
                        consoleOutput.append(">>> [提示] 目录读取成功，但没找到 .sh 文件\n")
                    } else {
                        consoleOutput.append(">>> 已刷新，找到 ${filtered.size} 个脚本\n")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append(">>> 发生错误: ${e.message}\n") }
            }
        }.start()
    }

    private fun executeWithTerminal(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 启动: $name\n--------------------\n"

        Thread {
            val outputLines = Collections.synchronizedList(mutableListOf<String>())
            // 沿用最稳的执行逻辑
            val result = Shell.cmd("sh $fullPath").to(outputLines, outputLines).exec()

            runOnUiThread {
                outputLines.forEach { consoleOutput.append("$it\n") }
                consoleOutput.append(if (result.isSuccess) "\n[OK]" else "\n[FAILED: ${result.code}]")
                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }.start()
    }
}

