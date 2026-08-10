package mega.privacy.android.core.nodecomponents.sheet.changelabel

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata

@Serializable
data class ChangeLabelBottomSheet(val nodeId: Long) : NavKey

@Serializable
data class ChangeLabelBottomSheetMultiple(val nodeIds: List<Long>) : NavKey

internal fun EntryProviderScope<NavKey>.changeLabelBottomSheetNavigation(
    onBack: () -> Unit,
) {
    entry<ChangeLabelBottomSheet>(metadata = bottomSheetMetadata()) {

        ChangeLabelBottomSheetContentM3(
            nodeId = NodeId(it.nodeId),
            onDismiss = onBack
        )
    }
    entry<ChangeLabelBottomSheetMultiple>(metadata = bottomSheetMetadata()) {
        ChangeLabelBottomSheetContentM3(
            nodeIds = it.nodeIds.map { id -> NodeId(id) },
            onDismiss = onBack
        )
    }
}
