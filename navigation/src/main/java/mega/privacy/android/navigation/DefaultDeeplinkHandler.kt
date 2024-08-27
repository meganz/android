package mega.privacy.android.navigation

import android.content.Context

/**
 * Default implementation of [DeeplinkHandler]
 */
class DefaultDeeplinkHandler(
    private val processors: Set<@JvmSuppressWildcards DeeplinkProcessor>,
) : DeeplinkHandler {

    override fun matches(deeplink: String): Boolean {
        processors.forEach {
            if (it.matches(deeplink)) {
                return true
            }
        }
        return false
    }

    override fun process(context: Context, deeplink: String): Boolean {
        processors.forEach {
            if (it.matches(deeplink)) {
                it.execute(context, deeplink)
                return true
            }
        }
        return false
    }
}