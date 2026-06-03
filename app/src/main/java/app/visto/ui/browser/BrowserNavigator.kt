package app.visto.ui.browser

import app.visto.core.model.DavPath

/**
 * Tracks the user's directory navigation as a back stack.
 *
 * The browser screen uses this to handle Android system back: empty stack
 * means we're at the configured root, and back exits the screen.
 */
class BrowserNavigator(initialPath: String = DavPath.ROOT) {

    private val stack: ArrayDeque<String> = ArrayDeque<String>().apply {
        addLast(DavPath.normalize(initialPath))
    }

    val currentPath: String
        get() = stack.last()

    val canGoBack: Boolean
        get() = stack.size > 1

    fun open(path: String) {
        val normalized = DavPath.normalize(path)
        if (normalized != stack.lastOrNull()) {
            stack.addLast(normalized)
        }
    }

    /** Returns the new current path, or null if there is nothing to pop. */
    fun back(): String? {
        if (stack.size <= 1) return null
        stack.removeLast()
        return stack.last()
    }
}
