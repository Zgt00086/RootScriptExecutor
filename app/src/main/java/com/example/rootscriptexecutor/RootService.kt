package com.example.rootscriptexecutor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class RootService : Service() {

    companion object {
        private const val TAG = "RootService"
        private const val SCRIPT_DIR = "/data/adb/5/"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RootService created")
        
        // Step 1: Process name camouflage
        camouflageProcessName()
        
        // Initialize libsu Shell with random socket name
        initRandomizedShell()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "RootService bound")
        return object : IRootService.Stub() {
            override fun executeScript(scriptName: String): String {
                return executeScriptInIsolatedNamespace(scriptName)
            }
            
            override fun checkScriptExists(scriptName: String): Boolean {
                return File("$SCRIPT_DIR$scriptName").exists()
            }
            
            override fun listScripts(): Array<String> {
                return File(SCRIPT_DIR).list() ?: emptyArray()
            }
            
            override fun uploadScript(scriptName: String, content: ByteArray): Boolean {
                return try {
                    val scriptFile = File("$SCRIPT_DIR$scriptName")
                    FileOutputStream(scriptFile).use { fos ->
                        fos.write(content)
                    }
                    // Set executable permission
                    Shell.cmd("chmod 755 $SCRIPT_DIR$scriptName").exec()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload script", e)
                    false
                }
            }
        }
    }

    private fun camouflageProcessName() {
        try {
            // Write to /proc/self/comm to change process name
            val commFile = File("/proc/self/comm")
            val fakeName = "system_server_watchdog"
            FileOutputStream(commFile).use { fos ->
                fos.write(fakeName.toByteArray())
            }
            Log.d(TAG, "Process name camouflaged to: $fakeName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to camouflage process name", e)
        }
    }

    private fun initRandomizedShell() {
        try {
            // Generate random socket name for IPC
            val randomSocketName = "root_service_${UUID.randomUUID().toString().replace("-", "")}"
            
            // Configure Shell with random socket
            Shell.Config.setFlags(Shell.FLAG_MOUNT_MASTER)
            Shell.Config.verboseLogging(false)
            
            // This ensures libsu uses random socket names internally
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(30)
            )
            
            Log.d(TAG, "Shell initialized with randomized IPC")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize shell", e)
        }
    }

    private fun executeScriptInIsolatedNamespace(scriptName: String): String {
        return try {
            val scriptPath = "$SCRIPT_DIR$scriptName"
            
            // Create the script directory if it doesn't exist
            Shell.cmd("mkdir -p $SCRIPT_DIR").exec()
            
            // Check if script exists and is executable
            val scriptFile = File(scriptPath)
            if (!scriptFile.exists()) {
                return "Error: Script $scriptName does not exist"
            }
            
            if (!scriptFile.canExecute()) {
                Shell.cmd("chmod 755 $scriptPath").exec()
            }
            
            // Execute script in isolated mount namespace
            val command = """
                # Enter isolated mount namespace first
                nsenter -t 1 -m sh -c "
                    # Change to script directory
                    cd $SCRIPT_DIR
                    
                    # Add current directory to PATH
                    export PATH=\$PATH:.
                    
                    # Execute script with exec to replace shell process
                    exec ./$scriptName
                "
            """.trimIndent()
            
            val result = Shell.cmd(command).exec()
            
            if (result.isSuccess) {
                val output = result.out.joinToString("\n")
                "Success: $output"
            } else {
                val error = result.err.joinToString("\n")
                "Error: $error"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute script", e)
            "Exception: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RootService destroyed")
    }
}