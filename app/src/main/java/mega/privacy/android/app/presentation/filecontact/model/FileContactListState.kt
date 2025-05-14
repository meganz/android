package mega.privacy.android.app.presentation.filecontact.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient

internal sealed interface FileContactListState {

    val folderName: String
    val folderId: NodeId

    /**
     * Loading state
     */
    @Immutable
    data class Loading(
        override val folderName: String,
        override val folderId: NodeId,
    ) : FileContactListState

    /**
     * Loading state
     */
    @Immutable
    data class Data(
        override val folderName: String,
        override val folderId: NodeId,
        val recipients: ImmutableList<ShareRecipient>,
        val shareRemovedEvent: StateEventWithContent<String>,
        val sharingInProgress: Boolean,
        val sharingCompletedEvent: StateEventWithContent<String>,
        val accessPermissions: ImmutableSet<AccessPermission>,
        val isContactVerificationWarningEnabled: Boolean,
    ) : FileContactListState
}