package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

@Serializable
data class NewTextFileDialogNavKey(
    val parentNodeId: NodeId,
) : DialogNavKey
