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

        btnRefresh.setOnClickListener { refreshScripts() }

        listView.setOnItemClickListener { _, _, position, _ ->
            val scriptName = scriptList[position]
            executeWithTerminal(scriptName)
        }

        refreshScripts()
    }

    private fun refreshScripts() {
        scriptList.clear()
        val result = Shell.cmd("ls /data/adb/5/ | grep .sh").exec()
        scriptList.addAll(result.out)
        adapter.notifyDataSetChanged()
    }

    private fun executeWithTerminal(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 正在启动: $name\n"

        Thread {
            // 这里是关键：实时捕获脚本输出
            val outputLines = Collections.synchronizedList(mutableListOf<String>())
            val result = Shell.cmd("sh $fullPath").to(outputLines, outputLines).exec()

            runOnUiThread {
                val finalContent = StringBuilder()
                outputLines.forEach { line ->
                    finalContent.append(line).append("\n")
                }
                
                if (result.isSuccess) {
                    finalContent.append("\n[OK] 脚本执行完毕")
                } else {
                    finalContent.append("\n[ERROR] 脚本异常退出，错误码: ${result.code}")
                }
                
                consoleOutput.text = finalContent.toString()
                // 自动滚动到黑屏最下方
                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }.start()
    }
}

