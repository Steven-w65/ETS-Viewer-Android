package com.etsviewer.core.resources

import com.etsviewer.core.service.ProtocolFailure
import com.etsviewer.core.service.digest
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.RandomAccessFile

class PackageProcessor {
    fun extract(archive: File, output: File): File {
        val magic = ByteArray(4)
        val magicSize = archive.inputStream().use { input ->
            var count = 0
            while (count < magic.size) {
                val read = input.read(magic, count, magic.size - count)
                if (read < 0) break
                count += read
            }
            count
        }
        if (magicSize != 4 || magic[0] != 0x50.toByte() || magic[1] != 0x4b.toByte() ||
            magic[2].toInt() !in listOf(3, 5, 7)) throw ProtocolFailure("Downloaded resource is not a ZIP archive")
        val footer = if (archive.length() >= 336) RandomAccessFile(archive, "r").use { input ->
            input.seek(input.length() - 336); ByteArray(336).also(input::readFully)
        } else ByteArray(0)
        val password = passwordFromFooter(footer)
        output.mkdirs()
        try {
            ZipFile(archive).use { zip ->
                if (!zip.isValidZipFile) throw ProtocolFailure("Invalid ZIP structure")
                if (zip.isEncrypted) {
                    if (password == null) throw ProtocolFailure("Encrypted archive requires an available ETS100 footer password; unsupported access format")
                    zip.setPassword(password.toCharArray())
                }
                val root = output.canonicalFile
                for (entry in zip.fileHeaders) {
                    val target = File(root, entry.fileName.replace('\\', '/')).canonicalFile
                    if (!target.toPath().startsWith(root.toPath()) || target == root) throw ProtocolFailure("ZIP entry escapes the resource cache")
                }
                zip.extractAll(output.absolutePath)
            }
            val content = contentFiles(archive, output).firstOrNull()
                ?: throw ProtocolFailure("Archive contains no content.json")
            return content
        } catch (e: Exception) {
            output.deleteRecursively()
            throw ProtocolFailure("Archive processing failed: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    /** ZIP entry order, not directory enumeration: all content files belong to this extraction root. */
    fun contentFiles(archive: File, output: File): List<File> {
        val root = output.canonicalFile
        return ZipFile(archive).use { zip ->
            if (!zip.isValidZipFile) throw ProtocolFailure("Invalid ZIP structure")
            zip.fileHeaders.filter { !it.isDirectory && it.fileName.replace('\\', '/').substringAfterLast('/') == "content.json" }
                .map { header ->
                    File(root, header.fileName.replace('\\', '/')).canonicalFile.also { file ->
                        if (!file.toPath().startsWith(root.toPath()) || !file.isFile) throw ProtocolFailure("Missing or invalid extracted content.json")
                    }
                }.distinctBy { it.path }
        }
    }

    companion object {
        fun passwordFromFooter(footer: ByteArray): String? {
            if (footer.size != 336) return null
            if (String(footer.copyOfRange(0, 8), Charsets.US_ASCII) != "MSTCHINA" &&
                String(footer.copyOfRange(144, 149), Charsets.US_ASCII) != "EPLAT") return null
            val first = digest("MD5", footer.copyOfRange(16, 144)).uppercase()
            return first + digest("MD5", first.toByteArray(Charsets.US_ASCII)).uppercase()
        }
    }
}
