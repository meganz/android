package mega.privacy.android.app.presentation.clouddrive

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import javax.inject.Inject

class CloudDriveDeepLinkHandler @Inject constructor() : DeepLinkHandler by Companion {

    companion object : PendingIntentHandler {
        internal const val FOLDER_NODE_ID_PARAM = "nodeId"
        internal const val HIGHLIGHTED_FILES_QUERY_PARAM = "highlightedFiles"

        override val authority = "cloudDrive"
        override suspend fun getNavKeysFromParameters(uri: Uri): List<NavKey> =
            listOf(
                CloudDriveNavKey(
                    uri.getQueryParameter(FOLDER_NODE_ID_PARAM)?.toLongOrNull() ?: -1L,
                    highlightedNodeNames = uri.getQueryParameters(HIGHLIGHTED_FILES_QUERY_PARAM),
                ),
            )

        /**
         * Returns an Uri to be used to navigate to cloud drive section.
         * This Uri is supposed to be used internally to navigate with PendingIntents only.
         * Examples
         * mega://cloudDrive?nodeId=48957
         *
         */
        fun getUriForCloudDriveSection(
            parentFolderNodeId: NodeId?,
            highlightedFiles: List<String>,
            uriBuilderFactory: () -> Uri.Builder = { Uri.Builder() },
        ): Uri =
            with(uriBuilderFactory()) {
                this.setSchemeAndAuthority()
                parentFolderNodeId?.longValue?.toString()?.let {
                    this.appendQueryParameter(FOLDER_NODE_ID_PARAM, it)
                }
                highlightedFiles.forEach {
                    this.appendQueryParameter(HIGHLIGHTED_FILES_QUERY_PARAM, it)
                }
                this.build()
            }

        /**
         * Returns a Pending Intent to open cloud drive section
         * @param context
         * @param folderNodeId the id of the folder to show
         * @param requestCode
         */
        fun getPendingIntentForCloudDriveSection(
            context: Context,
            folderNodeId: NodeId?,
            highlightedFiles: List<String>,
            requestCode: Int = 0,
        ): PendingIntent = MegaActivity.getPendingIntent(
            context,
            getUriForCloudDriveSection(folderNodeId, highlightedFiles, { Uri.Builder() }),
            requestCode,
        )
    }
}