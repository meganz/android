package mega.privacy.android.app.presentation.filestorage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.LegacyFileExplorerNavKey
import javax.inject.Inject

/**
 * Convert file storage deep links into [LegacyFileExplorerNavKey] if the given uri follows the file storage deep link format.
 */
class FileStorageDeepLinkHandler @Inject constructor() : DeepLinkHandler by Companion {

    companion object : PendingIntentHandler {

        internal const val PATH_QUERY_PARAM = "path"
        internal const val HIGHLIGHTED_FILES_QUERY_PARAM = "highlightedFiles"

        override val authority = "fileStorage"
        override suspend fun getNavKeysFromParameters(uri: Uri): List<NavKey> =
            listOf(
                LegacyFileExplorerNavKey(
                    uri.getQueryParameter(PATH_QUERY_PARAM)?.let { UriPath(it) },
                    uri.getQueryParameters(HIGHLIGHTED_FILES_QUERY_PARAM),
                ),
            )

        /**
         * Returns an Uri to be used to navigate to file storage screen
         * This Uri is supposed to be used internally to navigate with PendingIntents only.
         * Examples (not encoded)
         * mega://fileStorage?path=/folder&highlightedFiles=text1.txt&highlightedFiles=text2.txt
         * mega://fileStorage
         *
         */
        fun getUriForFileStorageSection(
            destination: String,
            highlightedFiles: List<String>,
            uriBuilderFactory: () -> Uri.Builder = { Uri.Builder() },
        ): Uri =
            with(uriBuilderFactory()) {
                this.setSchemeAndAuthority()
                this.appendQueryParameter(PATH_QUERY_PARAM, destination)
                highlightedFiles.forEach {
                    this.appendQueryParameter(HIGHLIGHTED_FILES_QUERY_PARAM, it)
                }
                this.build()
            }

        /**
         * Returns a Pending Intent to open file storage screen
         * @param context
         * @param destination the destination path to open
         * @param highlightedFiles the names of the files to be highlighted
         * @param requestCode
         */
        fun getPendingIntentForFileStorageSection(
            context: Context,
            destination: String,
            highlightedFiles: List<String>,
            requestCode: Int = 0,
        ): PendingIntent = MegaActivity.getPendingIntent(
            context,
            getUriForFileStorageSection(destination, highlightedFiles, { Uri.Builder() }),
            requestCode,
        )
    }
}