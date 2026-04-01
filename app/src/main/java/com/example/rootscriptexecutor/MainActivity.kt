package com.example.rootscriptexecutor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {

    private val scriptList = mutableListOf<String>()
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var etInput: EditText
    private lateinit var tvTitle: TextView
    
    private var scriptWriter: BufferedWriter? = null
    private var currentProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById(R.id.console_output)
        consoleScroll = findViewById(R.id.console_scroll)
        etInput = findViewById(R.id.et_input)
        tvTitle = findViewById(R.id.tv_title)
        val listView = findViewById<ListView>(R.id.script_list)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)
        val btnSend = findViewById<Button>(R.id.btn_send)

        btnRefresh.setOnClickListener { refreshScripts(listView) }
        
        btnSend.setOnClickListener {
            val input = etInput.text.toString()
            if (input.isNotEmpty()) {
                Thread {
                    try {
                        scriptWriter?.write(input + "\n")
                        scriptWriter?.flush()
                        runOnUiThread {
                            consoleOutput.append(">> $input\n")
                            etInput.setText("")
                        }
                    } catch (e: Exception) {
                        runOnUiThread { consoleOutput.append("[错误]: 无法发送输入\n") }
                    }
                }.start()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            startTerminalScript(scriptList[position])
        }

        refreshScripts(listView)
    }

    private fun refreshScripts(listView: ListView) {
        Thread {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls /data/adb/5/"))
                val output = p.inputStream.bufferedReader().readLines()
                val filtered = output.filter { it.endsWith(".sh") }
                runOnUiThread {
                    scriptList.clear()
                    scriptList.addAll(filtered)
                    listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList).apply {
                        // 强制设置白色文字，防止在黑底上看不见
                    }
                    tvTitle.text = "脚本目录: /data/adb/5/"
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun startTerminalScript(name: String) {
        val fullPath = "/data/adb/5/$name"
        tvTitle.text = "正在运行: $name"
        consoleOutput.text = "" // 清屏
        
        currentProcess?.destroy() // 杀掉之前的进程

        Thread {
            try {
                // 使用 su 开启持续会话
                val pb = ProcessBuilder("su")
                pb.redirectErrorStream(true) // 合并标准输出和错误输出
                val process = pb.start()
                currentProcess = process
                
                val writer = process.outputStream.bufferedWriter()
                scriptWriter = writer
                
                // 执行脚本命令
                writer.write("sh '$fullPath'\n")
                writer.flush()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                
                // 实时读取每一行，只要进程没死或缓冲区有数据就一直读
                while (true) {
                    line = reader.readLine() ?: break
                    val finalLine = line
                    runOnUiThread {
                        consoleOutput.append(finalLine + "\n")
                        // 模仿终端自动滚动
                        consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }
                
                process.waitFor()
                runOnUiThread { consoleOutput.append("\n--- 脚本执行完毕 ---\n") }

            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append("[启动失败]: ${e.message}\n") }
            }
        }.start()
    }
}

