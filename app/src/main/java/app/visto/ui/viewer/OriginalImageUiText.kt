package app.visto.ui.viewer

import java.util.Locale

internal object OriginalImageUiText {
    fun loadButtonLabel(sizeBytes: Long?): String {
        val size = sizeBytes?.takeIf { it > 0 }?.let(::formatBytes)
        return if (size == null) "加载原图" else "加载原图 · $size"
    }

    fun fileDetails(name: String, sizeBytes: Long?): String {
        val size = sizeBytes?.takeIf { it > 0 }?.let(::formatBytes)
        return if (size == null) name else "$name · $size"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return if (unit == 0) {
            "${bytes} B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unit])
        }
    }
}
