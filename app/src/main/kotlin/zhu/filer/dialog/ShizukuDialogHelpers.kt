package zhu.filer.dialog

import zhu.filer.util.ShizukuManager
import zhu.filer.util.createDir
import zhu.filer.util.createFile
import zhu.filer.util.deleteFile
import zhu.filer.util.renameTo
import zhu.filer.util.tryAction
import java.io.File

// 通过Shizuku删除
fun tryShizukuDelete(file: File): Boolean =
    ShizukuManager.tryAction { ShizukuManager.deleteFile(file.absolutePath) } ?: false

// 通过Shizuku创建文件
fun tryShizukuCreateFile(file: File): Boolean =
    ShizukuManager.tryAction { ShizukuManager.createFile(file.absolutePath) } ?: false

// 通过Shizuku创建目录
fun tryShizukuCreateDir(dir: File): Boolean =
    ShizukuManager.tryAction { ShizukuManager.createDir(dir.absolutePath) } ?: false

// 通过Shizuku重命名
fun tryShizukuRename(src: File, dest: File): Boolean =
    ShizukuManager.tryAction { ShizukuManager.renameTo(src.absolutePath, dest.absolutePath) } ?: false
