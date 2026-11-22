package app.visto.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Best-effort offline check used only to sharpen user-facing error messages.
 *
 * The app is read-only and entirely network-backed, so when the device has no
 * usable network every WebDAV and update call fails with the same generic
 * "network error" string. Asking this first lets the UI say "you are offline"
 * instead of blaming the server.
 *
 * Deliberately not a gate: callers should still attempt the request and only
 * use this to explain a failure, so a flaky ConnectivityManager never blocks a
 * working network.
 */
object NetworkStatus {

    @Volatile
    private var appContext: Context? = null

    /** Called once from [app.visto.VistoApplication.onCreate]. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Convenience overload so call sites do not need a Context in scope. */
    fun isOnline(): Boolean {
        val ctx = appContext ?: return true
        return isOnline(ctx)
    }

    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
