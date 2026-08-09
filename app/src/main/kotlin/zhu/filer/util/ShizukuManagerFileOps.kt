package zhu.filer.util

// 通过Shizuku列出目录（单次进程，批量获取所有文件信息）
fun ShizukuManager.listFilesWithDetails(path: String): List<ShizukuManager.ShizukuFileInfo>? {
    if (!hasPermission()) return null
    return try {
        // 单次 find -exec stat 调用，一次性获取所有文件的类型、大小、修改时间
        // 格式: perms\tsize\tmtime\tfullpath（文件名放最后，避免含制表符时解析错误）
        val result = exec("sh", "-c",
            "find \"\$1\" -maxdepth 1 -mindepth 1 -exec stat -c '%A\t%s\t%Y\t%n' {} + 2>/dev/null; true",
            "_", path)
        if (result.isBlank()) return emptyList()
        val infos = mutableListOf<ShizukuManager.ShizukuFileInfo>()
        for (line in result.trim().split("\n")) {
            if (line.isEmpty()) continue
            val parts = line.split("\t", limit = 4)
            if (parts.size < 4) continue
            val isDir = parts[0].startsWith("d")
            val size = if (isDir) 0L else (parts[1].toLongOrNull() ?: 0L)
            val time = (parts[2].toLongOrNull() ?: 0L) * 1000
            val fullPath = parts[3]
            val name = fullPath.substringAfterLast("/")
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
