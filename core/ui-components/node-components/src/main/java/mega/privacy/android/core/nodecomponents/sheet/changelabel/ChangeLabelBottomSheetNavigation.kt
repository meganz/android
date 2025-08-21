package mega.privacy.android.core.nodecomponents.sheet.changelabel

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.bottomsheet.megaBottomSheet

@Serializable
data class ChangeLabelBottomSheet(val nodeId: Long): NavKey

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