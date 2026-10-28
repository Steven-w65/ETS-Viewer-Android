package com.etsviewer.core.answers

import java.io.File
import java.net.URI

object MediaLocations {
    fun resolve(reference: String, contentDirectory: String, extractionDirectory: String): String? = runCatching {
        val uri = URI(reference.replace(" ", "%20"))
        if (uri.scheme == "https" || uri.scheme == "http") {
            require(!uri.host.isNullOrBlank())
            return reference.replaceFirst(Regex("^http://"), "https://")
        }
        require(uri.scheme == null && !reference.startsWith("//") && contentDirectory.isNotBlank() && extractionDirectory.isNotBlank())
        val root = File(extractionDirectory).canonicalFile
        val file = File(contentDirectory, uri.path.trimStart('/')).canonicalFile
        require(file.toPath().startsWith(root.toPath()) && file.isFile)
        file.toURI().toString()
    }.getOrNull()
}
