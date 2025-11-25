package mega.privacy.android.core.sharedcomponents.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Saver for NodeId? that saves/restores the longValue
 * Uses -1L as a sentinel value for null
 */
private val NodeIdSaver = Saver<NodeId?, Long>(
    save = { it?.longValue ?: -1L },
    restore = { NodeId(it).takeIf { id -> id.longValue != -1L } }
)

@Composable
fun rememberNodeId(
    nodeId: NodeId?,
) = rememberSaveable(stateSaver = NodeIdSaver) { mutableStateOf(nodeId) }