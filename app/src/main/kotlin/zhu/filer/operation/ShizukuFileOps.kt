package zhu.filer.operation

import zhu.filer.util.ShizukuManager
import zhu.filer.util.copyFile
import zhu.filer.util.deleteFile
import zhu.filer.util.renameTo
import zhu.filer.util.tryAction
import java.io.File

// 通过Shizuku移动文件
internal suspend fun tryShizukuMove(src: File, dest: File): Boolean =
    ShizukuManager.tryAction {
        if (ShizukuManager.renameTo(src.absolutePath, dest.absolutePath)) {
            true
        } else {
            if (ShizukuManager.copyFile(src.absolutePath, dest.absolutePath)) {
                ShizukuManager.deleteFile(src.absolutePath)
            } else {
                false
            }
        }
    } ?: false

// 通过Shizuku复制文件
internal suspend fun tryShizukuCopy(src: File, dest: File): Boolean =
    ShizukuManager.tryAction { ShizukuManager.copyFile(src.absolutePath, dest.absolutePath) } ?: false
