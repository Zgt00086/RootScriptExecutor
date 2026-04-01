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

        // 显式指定泛型类型 <T>，解决编译器的推断报错
        consoleOutput = findViewById<TextView>(R.id.console_output)
        consoleScroll = findViewById<ScrollView>(R.id.console_scroll)
        val listView = findViewById<ListView>(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
        listView.adapter = adapter

        btnRefresh.setOnClickListener { refreshScripts() }
        listView.setOnItemClickListener { _, _, position, _ ->
            executeWithTerminal(scriptList[position])
        }

        refreshScripts()
    }

    private fun refreshScripts() {
        Thread {
            try {
                // 确保 Root 环境激活
                Shell.getShell().isRoot
                val output = Shell.cmd("ls /data/adb/5/").exec().out
                val filtered = output.filter { it.endsWith(".sh") }

                runOnUiThread {
                    scriptList.clear()
                    scriptList.addAll(filtered)
                    adapter.notifyDataSetChanged()
                    
                    if (filtered.isEmpty()) {
                        consoleOutput.append(">>> 提示: /data/adb/5/ 目录下无脚本或无权限读取\n")
                    } else {
                        consoleOutput.append(">>> 刷新成功，找到 ${filtered.size} 个脚本\n")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append(">>> 刷新出错: ${e.message}\n") }
            }
        }.start()
    }

    private fun executeWithTerminal(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 正在执行: $name\n--------------------\n"

        Thread {
            val outputLines = Collections.synchronizedList(mutableListOf<String>())
            val result = Shell.cmd("sh $fullPath").to(outputLines, outputLines).exec()

            runOnUiThread {
                outputLines.forEach { consoleOutput.append("$it\n") }
                consoleOutput.append(if (result.isSuccess) "\n[√] 任务完成" else "\n[×] 任务失败: ${result.code}")
                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }.start()
    }
}

