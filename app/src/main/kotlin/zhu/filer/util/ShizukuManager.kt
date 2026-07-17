package zhu.filer.util

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

// Shizuku管理器
object ShizukuManager {

    // 是否安装了Shizuku
    fun isInstalled(): Boolean {
        return try {
            Class.forName("rikka.shizuku.ShizukuProvider")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    // Shizuku是否正在运行
    fun isRunning(): Boolean {
        if (!isInstalled()) return false
        return try {
            if (Shizuku.isPreV11()) return false
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    // 是否已授权
    fun hasPermission(): Boolean {
        if (!isRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // 是否以ROOT运行
    fun isRoot(): Boolean {
        if (!isRunning()) return false
        return try {
            Shizuku.getUid() == 0
        } catch (e: Exception) {
            false
        }
    }

    // 权限状态
    sealed class PermissionState {
        object NotInstalled : PermissionState()
        object NotRunning : PermissionState()
        object NoPermission : PermissionState()
        object Granted : PermissionState()
    }

    // 获取权限状态
    fun getPermissionState(): PermissionState {
        if (!isInstalled()) return PermissionState.NotInstalled
        if (!isRunning()) return PermissionState.NotRunning
        if (!hasPermission()) return PermissionState.NoPermission
        return PermissionState.Granted
    }

    // 请求权限
    fun requestPermission(requestCode: Int) {
        if (!isRunning()) return
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
        }
    }

    // 添加权限结果监听
    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (e: Exception) {
        }
    }

    // 移除权限结果监听
    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Exception) {
        }
    }

    // 通过反射调用Shizuku创建进程
    private fun newProcess(cmd: Array<out String>): Process? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, cmd, null, null) as? Process
        } catch (e: Exception) {
            null
        }
    }

    // 执行shell命令
    internal fun exec(vararg cmd: String): String {
        if (!hasPermission()) return ""
        val process = newProcess(cmd) ?: return ""
        val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
        process.waitFor()
        return if (output.isNotEmpty()) output else error
    }

    // 执行shell命令无输出
    internal fun execSilent(vararg cmd: String): Boolean {
        if (!hasPermission()) return false
        return try {
            val process = newProcess(cmd) ?: return false
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    // Shizuku文件信息
    data class ShizukuFileInfo(
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )
}

// 尝试执行Shizuku操作
inline fun <T> ShizukuManager.tryAction(block: () -> T): T? {
    if (!hasPermission()) return null
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}
