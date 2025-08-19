package mega.privacy.android.core.nodecomponents.sheet.changelabel

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.bottomsheet.megaBottomSheet

@Serializable
data class ChangeLabelBottomSheet(val nodeId: Long)

internal fun NavGraphBuilder.changeLabelBottomSheetNavigation(
    onBack: () -> Unit,
) {
    megaBottomSheet<ChangeLabelBottomSheet> {
        val args = it.toRoute<ChangeLabelBottomSheet>()

        ChangeLabelBottomSheetContentM3(
            nodeId = NodeId(args.nodeId),
            onDismiss = onBack
        )
    }
}