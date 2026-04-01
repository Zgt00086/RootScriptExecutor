package com.example.rootscriptexecutor

import android.os.Bundle
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
    private var scriptWriter: BufferedWriter? = null
    private var currentSort = 2 // 默认按日期排序

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleOutput = findViewById(R.id.console_output)
        consoleScroll = findViewById(R.id.console_scroll)
        etInput = findViewById(R.id.et_input)
        val listView = findViewById<ListView>(R.id.script_list)

        findViewById<Button>(R.id.btn_refresh).setOnClickListener { refresh() }
        findViewById<Button>(R.id.btn_sort).setOnClickListener { showSort() }
        findViewById<Button>(R.id.btn_send).setOnClickListener { send() }

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
                // 使用 ls -al 获取详细信息用于排序
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
            } catch (e: Exception) {}
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
        findViewById<ListView>(R.id.script_list).adapter = ArrayAdapter(this, R.layout.my_list_item, displayNames)
    }

    private fun send() {
        val txt = etInput.text.toString()
        if (txt.isNotEmpty()) {
            Thread {
                try {
                    scriptWriter?.write(txt + "\n")
                    scriptWriter?.flush()
                    runOnUiThread { 
                        consoleOutput.append(">> $txt\n")
                        etInput.setText("")
                    }
                } catch (e: Exception) {}
            }.start()
        }
    }

    private fun runScript(name: String) {
        val path = "/data/adb/5/$name"
        consoleOutput.text = ">>> START: $name\n"
        Thread {
            try {
                val pb = ProcessBuilder("su")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                scriptWriter = proc.outputStream.bufferedWriter()
                scriptWriter?.write("sh '$path'\n")
                scriptWriter?.flush()

                val reader = proc.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line
                    runOnUiThread {
                        consoleOutput.append(l + "\n")
                        consoleScroll.post { consoleScroll.fullScroll(android.view.View.FOCUS_DOWN) }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { consoleOutput.append("ERROR: ${e.message}\n") }
            }
        }.start()
    }
}

