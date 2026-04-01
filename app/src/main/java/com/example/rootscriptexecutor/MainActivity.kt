package com.example.rootscriptexecutor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {

    private val scriptList = mutableListOf<String>()
    private lateinit var listView: ListView
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var etInput: EditText
    
    // 核心：用于向脚本写入数据的输出流
    private var scriptWriter: BufferedWriter? = null
    private var currentProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById(R.id.console_output)
        consoleScroll = findViewById(R.id.console_scroll)
        listView = findViewById(R.id.script_list)
        etInput = findViewById(R.id.et_input)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh)
        val btnSend = findViewById<Button>(R.id.btn_send)

        btnRefresh.setOnClickListener { refreshScripts() }
        
        // 发送按钮点击事件
        btnSend.setOnClickListener {
            val input = etInput.text.toString()
            if (input.isNotEmpty() && scriptWriter != null) {
                Thread {
                    try {
                        scriptWriter?.write(input + "\n")
                        scriptWriter?.flush()
                        runOnUiThread {
                            consoleOutput.append(">>> 输入: $input\n")
                            etInput.setText("")
                        }
                    } catch (e: Exception) {
                        runOnUiThread { consoleOutput.append(">>> 发送失败: ${e.message}\n") }
                    }
                }.start()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            startInteractiveScript(scriptList[position])
        }

        refreshScripts()
    }

    private fun refreshScripts() {
        Thread {
            try {
                // 刷新逻辑保持简单稳健
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls /data/adb/5/"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readLines()
                val filtered = output.filter { it.endsWith(".sh") }

                runOnUiThread {
                    scriptList.clear()
                    scriptList.addAll(filtered)
                    listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptList)
                    consoleOutput.append(">>> 列表已刷新\n")
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append(">>> 刷新失败: ${e.message}\n") }
            }
        }.start()
    }

    private fun startInteractiveScript(name: String) {
        val fullPath = "/data/adb/5/$name"
        consoleOutput.text = ">>> 启动交互式脚本: $name\n--------------------\n"
        
        // 停止之前的进程（如果有）
        currentProcess?.destroy()

        Thread {
            try {
                // 使用 su 启动交互式进程
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh $fullPath"))
                currentProcess = process
                scriptWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                // 开启独立线程读取输出，防止阻塞
                Thread {
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            runOnUiThread {
                                consoleOutput.append(line + "\n")
                                consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                            }
                        }
                    } catch (e: Exception) {}
                }.start()

                // 同时也读取错误流
                Thread {
                    var errLine: String?
                    try {
                        while (errorReader.readLine().also { errLine = it } != null) {
                            runOnUiThread { consoleOutput.append("[ERR] $errLine\n") }
                        }
                    } catch (e: Exception) {}
                }.start()

                process.waitFor()
                runOnUiThread { consoleOutput.append("\n>>> 脚本已运行结束\n") }

            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append(">>> 启动失败: ${e.message}\n") }
            }
        }.start()
    }
}

