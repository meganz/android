package mega.privacy.android.app.presentation.zipbrowser

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey
import javax.inject.Inject

/**
 * Creates a Pending Intent to open zip screen
 */
class ZipBrowserPendingIntentMapper @Inject constructor() {

    /**
     * Returns a Pending Intent to open zip screen
     * @param context
     * @param zipFilePath the path of the zip file to open
     * @param nodeId
     */
    operator fun invoke(
        context: Context,
        zipFilePath: String?,
    ): PendingIntent = MegaActivity.getPendingIntentWithExtraDestination(
        context,
        LegacyZipBrowserNavKey(
            zipFilePath,
        ),
    )
}