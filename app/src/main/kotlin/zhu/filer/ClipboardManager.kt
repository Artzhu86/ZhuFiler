package zhu.filer

import java.io.File

// 剪贴板管理器
class ClipboardManager {
    private var files: List<File> = emptyList()
    private var action: String? = null

    // 是否有内容
    fun hasContent(): Boolean = files.isNotEmpty() && action != null

    // 获取文件列表
    fun getFiles(): List<File> = files

    // 是否剪切
    fun isCut(): Boolean = action == "cut"

    // 设置文件列表
    fun set(files: List<File>, isCut: Boolean) {
        this.files = files
        this.action = if (isCut) "cut" else "copy"
    }

    // 设置单个文件
    fun set(file: File, isCut: Boolean) {
        set(listOf(file), isCut)
    }

    // 清空剪贴板
    fun clear() {
        files = emptyList()
        action = null
    }
}
