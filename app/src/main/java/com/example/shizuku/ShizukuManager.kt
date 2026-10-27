package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShizukuStatus(
    val isAvailable: Boolean = false,
    val hasPermission: Boolean = false,
    val version: Int = 0,
    val uid: Int = -1,
    val isRoot: Boolean = false,
    val isAdb: Boolean = false,
    val seLinuxContext: String? = null
)

sealed class ExecutionEvent {
    data class OutputLine(val line: String, val isError: Boolean) : ExecutionEvent()
    data class Finished(val exitCode: Int, val durationMs: Long) : ExecutionEvent()
    data class Error(val message: String) : ExecutionEvent()
}

class ShizukuManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val SHIZUKU_REQUEST_CODE = 1001
        private const val MAX_OUTPUT_BYTES = 200 * 1024 // 200 KB cap
        private const val TAG = "ShizukuManager"
    }

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var currentProcess: Process? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus(isAvailable = false, hasPermission = false)
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                updateStatusWithPermission(granted)
            }
        }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Shizuku listeners", e)
            _status.value = ShizukuStatus(isAvailable = false, hasPermission = false)
        }
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Shizuku listeners", e)
        }
    }

    fun checkStatus() {
        val ping = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!ping) {
            _status.value = ShizukuStatus(isAvailable = false, hasPermission = false)
            return
        }

        val hasPermission = checkPermission()
        val version = runCatching { Shizuku.getVersion() }.getOrDefault(0)
        val uid = if (hasPermission) runCatching { Shizuku.getUid() }.getOrDefault(-1) else -1
        val isRoot = uid == 0
        val isAdb = uid == 2000
        val seLinux = if (hasPermission) runCatching { Shizuku.getSELinuxContext() }.getOrNull() else null

        _status.value = ShizukuStatus(
            isAvailable = true,
            hasPermission = hasPermission,
            version = version,
            uid = uid,
            isRoot = isRoot,
            isAdb = isAdb,
            seLinuxContext = seLinux
        )
    }

    fun checkPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "checkSelfPermission failed", e)
            false
        }
    }

    private fun updateStatusWithPermission(granted: Boolean) {
        val current = _status.value
        val uid = if (granted) runCatching { Shizuku.getUid() }.getOrDefault(-1) else -1
        val seLinux = if (granted) runCatching { Shizuku.getSELinuxContext() }.getOrNull() else null
        _status.value = current.copy(
            hasPermission = granted,
            uid = uid,
            isRoot = uid == 0,
            isAdb = uid == 2000,
            seLinuxContext = seLinux
        )
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    // Still request
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                } else {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    suspend fun executeCommand(
        cmd: String,
        onEvent: suspend (ExecutionEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            onEvent(ExecutionEvent.Error("Shizuku service is not running. Please start Shizuku first."))
            return@withContext
        }

        if (!checkPermission()) {
            onEvent(ExecutionEvent.Error("Shizuku permission not granted. Please grant permission."))
            return@withContext
        }

        val startTime = System.currentTimeMillis()
        var process: Process? = null

        try {
            val shCmd = arrayOf("sh", "-c", cmd)
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            process = newProcessMethod.invoke(null, shCmd, null, null) as Process
            currentProcess = process

            var totalBytes = 0
            var truncated = false

            val stdoutJob = launch {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        val lineBytes = currentLine.toByteArray().size
                        if (totalBytes + lineBytes <= MAX_OUTPUT_BYTES) {
                            totalBytes += lineBytes
                            onEvent(ExecutionEvent.OutputLine(currentLine, isError = false))
                        } else if (!truncated) {
                            truncated = true
                            onEvent(ExecutionEvent.OutputLine("... [Output truncated at 200 KB limit]", isError = false))
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Stdout reader ended", e)
                }
            }

            val stderrJob = launch {
                try {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        val lineBytes = currentLine.toByteArray().size
                        if (totalBytes + lineBytes <= MAX_OUTPUT_BYTES) {
                            totalBytes += lineBytes
                            onEvent(ExecutionEvent.OutputLine(currentLine, isError = true))
                        } else if (!truncated) {
                            truncated = true
                            onEvent(ExecutionEvent.OutputLine("... [Output truncated at 200 KB limit]", isError = true))
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Stderr reader ended", e)
                }
            }

            // Close stdin as we aren't writing interactive input
            runCatching { process.outputStream.close() }

            val exitCode = process.waitFor()
            stdoutJob.join()
            stderrJob.join()

            val duration = System.currentTimeMillis() - startTime
            onEvent(ExecutionEvent.Finished(exitCode, duration))
        } catch (e: InterruptedException) {
            val duration = System.currentTimeMillis() - startTime
            process?.destroy()
            onEvent(ExecutionEvent.Finished(exitCode = 130, durationMs = duration)) // 130 = SIGINT
        } catch (e: Exception) {
            Log.e(TAG, "Process execution failed", e)
            onEvent(ExecutionEvent.Error("Process execution error: ${e.localizedMessage ?: e.message}"))
        } finally {
            currentProcess = null
        }
    }

    fun stopCurrentProcess() {
        try {
            currentProcess?.destroy()
            currentProcess = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy process", e)
        }
    }
}
