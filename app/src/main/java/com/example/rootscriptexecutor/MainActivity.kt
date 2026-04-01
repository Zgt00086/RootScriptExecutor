package com.example.rootscriptexecutor

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import java.io.File

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var scriptNameEditText: EditText
    private lateinit var scriptContentEditText: EditText
    private lateinit var outputTextView: TextView
    private lateinit var executeButton: Button
    private lateinit var uploadButton: Button
    private lateinit var listButton: Button
    
    private var rootService: IRootService? = null
    private var isBound = false
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "RootService connected")
            rootService = IRootService.Stub.asInterface(service)
            isBound = true
            Toast.makeText(this@MainActivity, "Root service connected", Toast.LENGTH_SHORT).show()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "RootService disconnected")
            rootService = null
            isBound = false
            Toast.makeText(this@MainActivity, "Root service disconnected", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI components
        scriptNameEditText = findViewById(R.id.scriptNameEditText)
        scriptContentEditText = findViewById(R.id.scriptContentEditText)
        outputTextView = findViewById(R.id.outputTextView)
        executeButton = findViewById(R.id.executeButton)
        uploadButton = findViewById(R.id.uploadButton)
        listButton = findViewById(R.id.listButton)
        
        // Initialize libsu Shell
        initShell()
        
        // Bind to RootService
        bindRootService()
        
        // Set up button listeners
        executeButton.setOnClickListener {
            executeScript()
        }
        
        uploadButton.setOnClickListener {
            uploadScript()
        }
        
        listButton.setOnClickListener {
            listScripts()
        }
    }
    
    private fun initShell() {
        try {
            // Initialize libsu with minimal configuration
            Shell.Config.setFlags(Shell.FLAG_MOUNT_MASTER)
            Shell.Config.verboseLogging(false)
            Shell.enableVerboseLogging = false
            
            // Check root access
            Shell.getShell { shell ->
                if (shell.isRoot) {
                    Log.d(TAG, "Root access granted")
                    runOnUiThread {
                        Toast.makeText(this, "Root access available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "No root access")
                    runOnUiThread {
                        Toast.makeText(this, "No root access", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize shell", e)
        }
    }
    
    private fun bindRootService() {
        try {
            val intent = Intent(this, RootService::class.java)
            RootService.bind(intent, connection)
            Log.d(TAG, "Attempting to bind RootService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind RootService", e)
            Toast.makeText(this, "Failed to bind root service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun executeScript() {
        val scriptName = scriptNameEditText.text.toString().trim()
        if (scriptName.isEmpty()) {
            Toast.makeText(this, "Please enter script name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isBound || rootService == null) {
            Toast.makeText(this, "Root service not connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        Thread {
            try {
                val result = rootService!!.executeScript(scriptName)
                runOnUiThread {
                    outputTextView.text = "Execution Result:\n$result"
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to execute script", e)
                runOnUiThread {
                    outputTextView.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun uploadScript() {
        val scriptName = scriptNameEditText.text.toString().trim()
        val scriptContent = scriptContentEditText.text.toString().trim()
        
        if (scriptName.isEmpty()) {
            Toast.makeText(this, "Please enter script name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (scriptContent.isEmpty()) {
            Toast.makeText(this, "Please enter script content", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isBound || rootService == null) {
            Toast.makeText(this, "Root service not connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        Thread {
            try {
                val success = rootService!!.uploadScript(scriptName, scriptContent.toByteArray())
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@MainActivity, "Script uploaded successfully", Toast.LENGTH_SHORT).show()
                        outputTextView.text = "Script '$scriptName' uploaded to /data/adb/5/"
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to upload script", Toast.LENGTH_SHORT).show()
                        outputTextView.text = "Failed to upload script"
                    }
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to upload script", e)
                runOnUiThread {
                    outputTextView.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun listScripts() {
        if (!isBound || rootService == null) {
            Toast.makeText(this, "Root service not connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        Thread {
            try {
                val scripts = rootService!!.listScripts()
                runOnUiThread {
                    if (scripts.isNotEmpty()) {
                        val scriptList = scripts.joinToString("\n")
                        outputTextView.text = "Available scripts in /data/adb/5/:\n$scriptList"
                    } else {
                        outputTextView.text = "No scripts found in /data/adb/5/"
                    }
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to list scripts", e)
                runOnUiThread {
                    outputTextView.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try {
                unbindService(connection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind service", e)
            }
            isBound = false
        }
    }
}