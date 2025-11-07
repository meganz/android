package mega.privacy.android.app.presentation.filestorage

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.navigation.destination.LegacyFileExplorerNavKey
import javax.inject.Inject

/**
 * Create a Pending Intent to open file storage screen
 */
class FileStoragePendingIntentMapper @Inject constructor() {

    /**
     * Returns a Pending Intent to open file storage screen
     * @param context
     * @param destination the destination path to open
     * @param highlightedFiles the names of the files to be highlighted
     */
    operator fun invoke(
        context: Context,
        destination: String,
        highlightedFiles: List<String>,
    ): PendingIntent = MegaActivity.getPendingIntentWithExtraDestination(
        context,
        LegacyFileExplorerNavKey(destination, highlightedFiles),
    )
}