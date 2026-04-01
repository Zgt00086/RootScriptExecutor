package com.example.rootscriptexecutor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private val scriptList = mutableListOf<String>()
    private lateinit var listView: ListView
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById<TextView>(R.id.console_output)
        consoleScroll = findViewById<ScrollView>(R.id.console_scroll)
        listView = findViewById<ListView>(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)

        btnRefresh.setOnClickListener { refreshScripts() }
        listView.setOnItemClickListener { _, _, position, _ ->
            executeWithTerminal(scriptList[position])
        }

        refreshScripts()
    }

    private fun refreshScripts() {
        Thread {
            try {
                Shell.getShell().isRoot
                val output = Shell.cmd("ls /data/adb/5/").exec().out
                val filtered = output.filter { it.endsWith(".sh") }

                runOnUiThread {
                    scriptList.clear()
                    scriptList.addAll(filtered)
                    
                    // 核心修复：重新创建适配器并绑定，解决 UI 假死不显示的问题
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
                    listView.adapter = adapter
                    
                    if (filtered.isEmpty()) {
                        consoleOutput.append(">>> 目录读取为空，请检查权限和文件\n")
                    } else {
                        consoleOutput.append(">>> 成功刷新: 已找到 ${filtered.size} 个脚本\n")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append(">>> 刷新错误: ${e.message}\n") }
            }
        }.start()
    }

    private fun executeWithTerminal(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 正在运行: $name\n--------------------\n"

        Thread {
            val outputLines = Collections.synchronizedList(mutableListOf<String>())
            val result = Shell.cmd("sh $fullPath").to(outputLines, outputLines).exec()

            runOnUiThread {
                outputLines.forEach { consoleOutput.append("$it\n") }
                consoleOutput.append(if (result.isSuccess) "\n[√] 完成" else "\n[×] 失败: ${result.code}")
                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }.start()
    }
}

