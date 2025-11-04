package mega.privacy.android.app.presentation.zipbrowser

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.DeepLinkHandler
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey
import javax.inject.Inject

/**
 * Convert zip deep links into [LegacyZipBrowserNavKey] if the given uri follows the zip deep link format.
 */
class ZipBrowserDeepLinkHandler @Inject constructor() : DeepLinkHandler by Companion {

    companion object : PendingIntentHandler {

        internal const val PATH_QUERY_PARAM = "path"
        internal const val NODE_ID_QUERY_PARAM = "nodeId"

        override val authority: String = "zipBrowser"

        override fun getNavKeysFromParameters(uri: Uri): List<NavKey> =
            listOf(
                LegacyZipBrowserNavKey(
                    uri.getQueryParameter(PATH_QUERY_PARAM),
                    uri.getQueryParameter(NODE_ID_QUERY_PARAM)?.toLongOrNull()?.let { NodeId(it) }
                ),
            )

        /**
         * Returns an Uri to be used to navigate to zip screen.
         * This Uri is supposed to be used internally to navigate with PendingIntents only.
         * Examples
         * mega://zipBrowser?path=downloads
         * mega://zipBrowser
         *
         */
        fun getUriForZipBrowserSection(
            zipFilePath: String?,
            nodeId: NodeId?,
            uriBuilderFactory: () -> Uri.Builder = { Uri.Builder() },
        ): Uri =
            with(uriBuilderFactory()) {
                this.setSchemeAndAuthority()
                zipFilePath?.let { this.appendQueryParameter(PATH_QUERY_PARAM, it) }
                nodeId?.let {
                    this.appendQueryParameter(
                        NODE_ID_QUERY_PARAM,
                        it.longValue.toString()
                    )
                }
                this.build()
            }

        /**
         * Returns a Pending Intent to open zip screen
         * @param context
         * @param zipFilePath the path of the zip file to open
         * @param nodeId
         * @param requestCode
         */
        fun getPendingIntentForZipBrowserSection(
            context: Context,
            zipFilePath: String?,
            nodeId: NodeId? = null,
            requestCode: Int = 0,
        ): PendingIntent = MegaActivity.getPendingIntent(
            context,
            getUriForZipBrowserSection(zipFilePath, nodeId, { Uri.Builder() }),
            requestCode,
        )
    }
}