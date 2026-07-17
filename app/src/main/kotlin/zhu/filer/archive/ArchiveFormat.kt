package zhu.filer.archive

// 归档格式枚举
enum class ArchiveFormat(val extension: String) {
    ZIP("zip"),
    SEVEN_ZIP("7z"),
    TAR_GZ("tar.gz"),
    TAR_XZ("tar.xz")
}

// 去除已知归档扩展名
internal fun stripKnownArchiveExt(name: String): String {
    for (fmt in ArchiveFormat.entries) {
        val suffix = ".${fmt.extension}"
        if (name.endsWith(suffix, ignoreCase = true)) return name.dropLast(suffix.length)
    }
    return name
}

// 归档需要密码异常
class ArchivePasswordRequiredException(message: String = "Password required") : Exception(message)

// 归档密码错误异常
class WrongArchivePasswordException(message: String = "Wrong password") : Exception(message)
