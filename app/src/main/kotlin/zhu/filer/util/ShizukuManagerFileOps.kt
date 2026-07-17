package zhu.filer.util

// 通过Shizuku列出目录
fun ShizukuManager.listFilesWithDetails(path: String): List<ShizukuManager.ShizukuFileInfo>? {
    if (!hasPermission()) return null
    return try {
        val result = exec("ls", "-1", "-A", "-p", path)
        if (result.isBlank()) return emptyList()
        val infos = mutableListOf<ShizukuManager.ShizukuFileInfo>()
        for (line in result.trim().split("\n")) {
            if (line.isEmpty()) continue
            val isDir = line.endsWith("/")
            val name = if (isDir) line.dropLast(1) else line
            val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
            val size = if (isDir) 0L else length(fullPath)
            val time = lastModified(fullPath)
            infos.add(ShizukuManager.ShizukuFileInfo(name, isDir, size, time))
        }
        infos
    } catch (e: Exception) {
        null
    }
}

// 通过Shizuku删除文件
fun ShizukuManager.deleteFile(path: String): Boolean {
    return execSilent("rm", "-rf", path)
}

// 通过Shizuku创建文件
fun ShizukuManager.createFile(path: String): Boolean {
    return execSilent("touch", path)
}

// 通过Shizuku创建目录
fun ShizukuManager.createDir(path: String): Boolean {
    return execSilent("mkdir", path)
}

// 通过Shizuku重命名
fun ShizukuManager.renameTo(oldPath: String, newPath: String): Boolean {
    return execSilent("mv", oldPath, newPath)
}

// 通过Shizuku复制文件
fun ShizukuManager.copyFile(srcPath: String, destPath: String): Boolean {
    return execSilent("cp", "-r", srcPath, destPath)
}

// 通过Shizuku获取文件大小
fun ShizukuManager.length(path: String): Long {
    if (!hasPermission()) return 0L
    return try {
        val result = exec("stat", "-c", "%s", path)
        result.trim().toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    }
}

// 通过Shizuku获取最后修改时间
fun ShizukuManager.lastModified(path: String): Long {
    if (!hasPermission()) return 0L
    return try {
        val result = exec("stat", "-c", "%Y", path)
        result.trim().toLongOrNull()?.times(1000) ?: 0L
    } catch (e: Exception) {
        0L
    }
}
