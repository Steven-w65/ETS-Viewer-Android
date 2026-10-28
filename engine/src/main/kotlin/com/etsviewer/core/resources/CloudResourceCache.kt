package com.etsviewer.core.resources

import com.etsviewer.core.service.digest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class CloudResourceCache(val root: File) {
    internal val mutex = Mutex()
    internal fun slot(url: String) = File(root, digest("SHA-256", url.toByteArray(Charsets.UTF_8)))
    suspend fun clear() = mutex.withLock {
        check(root.canonicalFile != root.canonicalFile.parentFile) { "Invalid cache root" }
        if (root.exists() && !root.deleteRecursively()) error("Unable to clear resource cache")
        check(root.mkdirs() || root.isDirectory) { "Unable to recreate resource cache" }
    }
    fun sizeBytes() = if (root.exists()) root.walkTopDown().filter(File::isFile).sumOf(File::length) else 0L
}
