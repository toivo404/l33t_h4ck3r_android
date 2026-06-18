package com.example.l33t_h4ck3r_android

object FileTextDetector {
    private val supportedExtensions = setOf("html", "css", "js", "json", "txt", "md", "xml", "csv")

    fun isTextLike(node: SafFileNode): Boolean {
        val extension = node.name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in supportedExtensions || node.mimeType?.startsWith("text/") == true
    }

    fun extensionOf(fileName: String): String =
        fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    fun mimeTypeFor(fileName: String): String =
        when (extensionOf(fileName)) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "md" -> "text/markdown"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "xml" -> "application/xml"
            else -> "text/plain"
        }
}
