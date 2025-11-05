package mega.privacy.android.app.presentation.transfers.navigation

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.TransfersNavKey
import javax.inject.Inject

/**
 * Convert transfer deep links into [TransfersNavKey] if the given uri follows the transfer deep link format.
 */
class TransferDeepLinkHandler @Inject constructor() : DeepLinkHandler by Companion {

    companion object : PendingIntentHandler {
        internal const val TAB_QUERY_PARAM = "tab"

        override val authority = "trans"

        override suspend fun getNavKeysFromParameters(uri: Uri): List<NavKey> = listOf(
            TransfersNavKey(
                uri.getQueryParameter(TAB_QUERY_PARAM)
                    ?.let { TransfersNavKey.Tab.valueOf(it) })
        )

        /**
         * Returns an Uri to be used to navigate to transfer section.
         * This Uri is supposed to be used internally to navigate with PendingIntents only.
         * Examples
         * mega://trans?tab=Completed
         * mega://trans
         *
         */
        fun getUriForTransfersSection(
            tab: TransfersNavKey.Tab? = null,
            uriBuilderFactory: () -> Uri.Builder = { Uri.Builder() },
        ): Uri =
            with(uriBuilderFactory()) {
                this.setSchemeAndAuthority()
                tab?.let { this.appendQueryParameter(TAB_QUERY_PARAM, tab.toString()) }
                this.build()
            }

        /**
         * Returns a Pending Intent to open transfer section in the specified tab
         * @param context
         * @param tab the transfers tab that should be selected, if null view logic will decide which is the best tab to show
         * @param requestCode
         */
        fun getPendingIntentForTransfersSection(
            context: Context,
            tab: TransfersNavKey.Tab? = null,
            requestCode: Int = 0,
        ): PendingIntent = MegaActivity.getPendingIntent(
            context,
            getUriForTransfersSection(tab, { Uri.Builder() }),
            requestCode,
        )
    }
}