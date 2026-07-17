package zhu.filer.archive

import java.io.FileInputStream
import java.io.InputStream
import net.sf.sevenzipjbinding.IArchiveOpenCallback
import net.sf.sevenzipjbinding.ICryptoGetTextPassword
import net.sf.sevenzipjbinding.IOutCreateCallback
import net.sf.sevenzipjbinding.IOutItemAllFormats
import net.sf.sevenzipjbinding.ISequentialInStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.InputStreamSequentialInStream
import net.sf.sevenzipjbinding.impl.OutItemFactory

// 打开归档回调
internal class OpenCallback(val password: String?) : IArchiveOpenCallback, ICryptoGetTextPassword {
    internal var passwordRequested = false
    // 设置总进度
    override fun setTotal(files: Long?, bytes: Long?) {}
    // 设置已完成进度
    override fun setCompleted(files: Long?, bytes: Long?) {}
    // 获取解压密码
    override fun cryptoGetTextPassword(): String {
        if (password.isNullOrEmpty()) {
            passwordRequested = true
            throw SevenZipException("Password required to open archive")
        }
        return password
    }
}

// 创建归档回调
internal open class CreateCallback(
    val items: List<SourceItem>,
    private val onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)? = null
) : IOutCreateCallback<IOutItemAllFormats> {
    internal var failed = false
    // 获取条目信息
    override fun getItemInformation(
        index: Int,
        outItemFactory: OutItemFactory<IOutItemAllFormats>
    ): IOutItemAllFormats {
        val it = items[index]
        val out = outItemFactory.createOutItem()
        out.setPropertyPath(it.pathInArchive)
        if (it.isDirectory) {
            out.setPropertyAttributes(PropID.AttributesBitMask.FILE_ATTRIBUTE_DIRECTORY)
            out.setDataSize(0L)
        } else {
            out.setDataSize(it.file.length())
        }
        return out
    }

    // 获取输入流
    override fun getStream(index: Int): ISequentialInStream? {
        val it = items[index]
        if (it.isDirectory) {
            onProgress?.invoke(it.pathInArchive, 0L, 0L, index, items.size)
            return null
        }
        val total = it.file.length()
        onProgress?.invoke(it.pathInArchive, 0L, total, index, items.size)
        val counting = CountingInputStream(FileInputStream(it.file), total) { read ->
            onProgress?.invoke(it.pathInArchive, read, total, index, items.size)
        }
        return InputStreamSequentialInStream(counting)
    }

    // 设置总进度
    override fun setTotal(total: Long) {}
    // 设置已完成进度
    override fun setCompleted(complete: Long) {}
    // 设置操作结果
    override fun setOperationResult(operationResultOk: Boolean) {
        if (!operationResultOk) failed = true
    }
}

// 带密码的创建回调
internal class CreateCallbackWithPassword(
    items: List<SourceItem>,
    private val password: String,
    onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)? = null
) : CreateCallback(items, onProgress), ICryptoGetTextPassword {
    // 返回归档密码
    override fun cryptoGetTextPassword(): String = password
}

// 计数输入流
internal class CountingInputStream(
    private val wrapped: InputStream,
    val totalBytes: Long,
    private val onRead: (Long) -> Unit
) : InputStream() {
    private var bytesRead = 0L
    private var lastReportedPercent = -1
    // 读取单个字节
    override fun read(): Int {
        val b = wrapped.read()
        if (b != -1) {
            bytesRead++
            report()
        }
        return b
    }
    // 读取字节数组
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = wrapped.read(b, off, len)
        if (n > 0) {
            bytesRead += n
            report()
        }
        return n
    }
    // 上报读取进度
    private fun report() {
        val percent = if (totalBytes > 0) ((bytesRead * 100 / totalBytes).toInt()) else 100
        if (percent == 100 || percent != lastReportedPercent) {
            lastReportedPercent = percent
            onRead(bytesRead)
        }
    }
    // 获取可用字节数
    override fun available(): Int = wrapped.available()
    // 关闭流
    override fun close() = wrapped.close()
}
