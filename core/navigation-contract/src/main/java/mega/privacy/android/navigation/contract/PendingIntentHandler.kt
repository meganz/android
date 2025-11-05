package mega.privacy.android.navigation.contract

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler

/**
 * Helper interface for handling PendingIntents Uri's that are not intended to be handled as Deep links, only pending intents.
 */
interface PendingIntentHandler : DeepLinkHandler {

    /**
     * Scheme of the Uri for the PendingIntent Uri, default to "mega://"
     */
    val scheme: String get() = DEFAULT_SCHEME_FOR_PENDING_INTENTS

    /**
     * Authority of the Uri for the PendingIntent, it identifies the destination of this PendingIntent
     */
    val authority: String

    override suspend fun getNavKeysFromUri(uri: Uri): List<NavKey>? {
        return if (uri.scheme == scheme && uri.authority == authority) {
            getNavKeysFromParameters(uri)
        } else null
    }

    /**
     * Helper class to get the NavKey list from the Uri parameters once scheme and authority matches the specific values for this case
     */
    suspend fun getNavKeysFromParameters(uri: Uri): List<NavKey>?


    /**
     * Helper extension to set scheme and authority for Uri's of this type
     */
    fun Uri.Builder.setSchemeAndAuthority() {
        this.scheme(scheme)
        this.authority(authority)
    }

    companion object {
        /**
         * As we don't have deep links for this, only pending intents, let's use a custom scheme
         */
        const val DEFAULT_SCHEME_FOR_PENDING_INTENTS = "mega"
    }
}