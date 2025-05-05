package mega.privacy.android.app.presentation.filecontact.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.ShareRecipient

@Immutable
data class FileContactListState(
    val folderName: String,
    val folderId: NodeId,
    val recipients: ImmutableList<ShareRecipient>,
    val shareRemovedEvent: StateEventWithContent<String>,
    val sharingInProgress: Boolean,
    val sharingCompletedEvent: StateEventWithContent<String>,
)