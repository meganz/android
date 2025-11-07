package mega.privacy.android.app.presentation.clouddrive

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import javax.inject.Inject

/**
 * Creates a Pending Intent to open cloud drive section
 */
class CloudDrivePendingIntentMapper @Inject constructor() {

    /**
     * Returns a Pending Intent to open cloud drive section
     * @param context
     * @param folderNodeId the id of the folder to show
     */
    operator fun invoke(
        context: Context,
        folderNodeId: NodeId?,
        highlightedFiles: List<String>,
    ): PendingIntent = MegaActivity.getPendingIntentWithExtraDestination(
        context = context,
        navKey = CloudDriveNavKey(
            nodeHandle = folderNodeId?.longValue ?: -1,
            highlightedNodeNames = highlightedFiles,
        ),
    )
}
