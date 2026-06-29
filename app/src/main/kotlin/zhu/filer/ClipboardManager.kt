package zhu.filer

import java.io.File

class ClipboardManager {
    private var file: File? = null
    private var action: String? = null

    fun hasContent(): Boolean = file != null && action != null
    fun getFile(): File? = file
    fun isCut(): Boolean = action == "cut"
    fun set(file: File, isCut: Boolean) {
        this.file = file
        this.action = if (isCut) "cut" else "copy"
    }
    fun clear() {
        file = null
        action = null
    }
}