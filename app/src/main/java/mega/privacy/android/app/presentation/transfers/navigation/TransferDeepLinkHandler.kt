package mega.privacy.android.app.presentation.transfers.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.navigation.destination.TransfersNavKey
import javax.inject.Inject

/**
 * Convert transfer deep links into [TransfersNavKey] if the given uri follows the transfer deep link format.
 */
class TransferDeepLinkHandler @Inject constructor() {

    fun getNavKeysFromUri(uri: Uri): List<NavKey>? {
        return if (uri.scheme == "mega" && uri.authority == TRANSFER_ROUTE) {
            listOf(
                TransfersNavKey(
                    uri.getQueryParameter(TAB_QUERY_PARAM)
                        ?.let { TransfersNavKey.Tab.valueOf(it) })
            )
        } else null
    }

    companion object {
        //as we don't have deep links for this, only pending intents, let's use a custom scheme
        internal const val TRANSFER_SCHEME = "mega"
        internal const val TRANSFER_ROUTE = "trans"
        internal const val TAB_QUERY_PARAM = "tab"

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
                this.scheme(TRANSFER_SCHEME)
                this.authority(TRANSFER_ROUTE)
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
        ): PendingIntent {
            val intent = Intent(
                Intent.ACTION_VIEW,
                getUriForTransfersSection(tab, { Uri.Builder() }),
                context,
                MegaActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}