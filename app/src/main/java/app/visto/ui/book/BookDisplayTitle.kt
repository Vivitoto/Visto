package app.visto.ui.book

private val DisplayTitleExtensions = setOf("txt", "md", "epub")
private val ChineseBookTitlePattern = Regex("《([^》]+)》")

internal fun bookDisplayTitle(value: String): String {
    val fileName = value.trim().substringAfterLast('/').substringAfterLast('\\')
    val titleWithoutExtension = stripSupportedBookExtension(fileName)
    val mainTitle = ChineseBookTitlePattern.find(titleWithoutExtension)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    return mainTitle ?: titleWithoutExtension
}

private fun stripSupportedBookExtension(fileName: String): String {
    val extension = supportedBookExtension(fileName) ?: return fileName
    val lastDot = fileName.lastIndexOf('.')
    if (lastDot <= 0) return fileName
    return fileName.dropLast(extension.length + 1)
        .trimEnd()
        .ifBlank { fileName }
}

private fun supportedBookExtension(value: String): String? {
    val lastDot = value.lastIndexOf('.')
    if (lastDot <= 0 || lastDot == value.lastIndex) return null
    val extension = value.substring(lastDot + 1).lowercase()
    return extension.takeIf { it in DisplayTitleExtensions }
}
