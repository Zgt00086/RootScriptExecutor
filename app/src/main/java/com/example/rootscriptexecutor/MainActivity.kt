package com.example.rootscriptexecutor

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.util.*

data class ScriptFile(val name: String, val size: Long, val date: String)

class MainActivity : AppCompatActivity() {

    private val scripts = mutableListOf<ScriptFile>()
    private val displayNames = mutableListOf<String>()
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var etInput: EditText
    private lateinit var listView: ListView
    private var scriptWriter: BufferedWriter? = null
    private var currentSort = 2
    
    // 核心状态：记录脚本是否还在运行
    private var isProcessAlive = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById(R.id.console_output)
        consoleScroll = findViewById(R.id.console_scroll)
        etInput = findViewById(R.id.et_input)
        listView = findViewById(R.id.script_list)

        findViewById<Button>(R.id.btn_refresh)?.setOnClickListener { refresh() }
        findViewById<Button>(R.id.btn_sort)?.setOnClickListener { showSort() }
        findViewById<Button>(R.id.btn_send)?.setOnClickListener { send() }

        // 神级交互：监听软键盘的“回车”或“发送”键
        etInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                send()
                true
            } else {
                false
            }
        }

        listView.setOnItemClickListener { _, _, i, _ -> runScript(scripts[i].name) }

        refresh()
    }

    private fun showSort() {
        val options = arrayOf("按名称", "按大小", "按日期")
        AlertDialog.Builder(this).setTitle("排序方式").setSingleChoiceItems(options, currentSort) { d, w ->
            currentSort = w
            applySort()
            d.dismiss()
        }.show()
    }

    private fun refresh() {
        Thread {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls -al /data/adb/5/"))
                val lines = p.inputStream.bufferedReader().readLines()
                val newList = mutableListOf<ScriptFile>()
                lines.forEach { line ->
                    if (line.endsWith(".sh")) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 8) {
                            val size = parts[4].toLongOrNull() ?: 0L
                            val date = parts[5] + " " + parts[6]
                            newList.add(ScriptFile(parts.last(), size, date))
                        }
                    }
                }
                runOnUiThread {
                    scripts.clear()
                    scripts.addAll(newList)
                    applySort()
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append("刷新异常: ${e.message}\n") }
            }
        }.start()
    }

    private fun applySort() {
        when(currentSort) {
            0 -> scripts.sortBy { it.name }
            1 -> scripts.sortByDescending { it.size }
            2 -> scripts.sortByDescending { it.date }
        }
        displayNames.clear()
        scripts.forEach { displayNames.add(it.name) }
        listView.adapter = ArrayAdapter(this, R.layout.my_list_item, displayNames)
    }

    private fun send() {
        // 如果脚本已经跑完了，此时点击发送或敲回车，就执行“清屏关闭”操作
        if (!isProcessAlive) {
            runOnUiThread {
                consoleOutput.text = "终端已重置，等待新指令...\n"
                etInput.setText("")
            }
            return
        }

        // 如果脚本还在跑，就正常发送指令
        val txt = etInput.text.toString()
        Thread {
            try {
                scriptWriter?.write(txt + "\n")
                scriptWriter?.flush()
                runOnUiThread { 
                    if(txt.isNotEmpty()) consoleOutput.append(">> $txt\n")
                    etInput.setText("")
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun runScript(name: String) {
        val path = "/data/adb/5/$name"
        isProcessAlive = true // 标记进程开始
        runOnUiThread { 
            consoleOutput.text = ">>> START: $name\n" 
            etInput.setText("")
        }

        Thread {
            try {
                // 使用 su -c 把赋权和执行写在同一行，执行完进程会自动自然死亡
                val pb = ProcessBuilder("su", "-c", "chmod 777 '$path' && '$path'")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                scriptWriter = proc.outputStream.bufferedWriter()

                val reader = proc.inputStream.bufferedReader()
                var line: String?
                // 实时读取，直到进程结束
                while (reader.readLine().also { line = it } != null) {
                    val l = line
                    runOnUiThread {
                        consoleOutput.append(l + "\n")
                        consoleScroll.post { consoleScroll.fullScroll(View.FOCUS_DOWN) }
                    }
                }
                
                proc.waitFor() // 彻底等待底层进程死透
                isProcessAlive = false // 标记进程结束
                
                // 打印 MT 管理器同款提示语
                runOnUiThread {
                    consoleOutput.append("\n[进程已结束 - 按回车或发送键关闭]\n")
                    consoleScroll.post { consoleScroll.fullScroll(View.FOCUS_DOWN) }
                }

            } catch (e: Exception) {
                isProcessAlive = false
                runOnUiThread { consoleOutput.append("\nERROR: ${e.message}\n[进程异常 - 按回车或发送键关闭]\n") }
            }
        }.start()
    }
}

