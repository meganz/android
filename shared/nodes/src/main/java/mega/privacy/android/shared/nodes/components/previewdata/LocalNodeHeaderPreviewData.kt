package mega.privacy.android.shared.nodes.components.previewdata

import androidx.compose.runtime.compositionLocalOf
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeHeaderItemViewModel

/**
 * When non-null (typically set via [androidx.compose.runtime.CompositionLocalProvider] in @Preview),
 * [NodeViewWithHeader] uses this state instead of [NodeHeaderItemViewModel], so previews work
 * without a Hilt [androidx.lifecycle.ViewModelStoreOwner]. Production code leaves the default null.
 */
val LocalNodeHeaderPreviewData = compositionLocalOf<NodeHeaderItemUiState.Data?> { null }
