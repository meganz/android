package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.CloudDrive
import javax.inject.Inject

/**
 * Options provider for Recents bucket source type.
 *
 * Uses the same menu sets as [CloudDriveMenuOptionsProvider]: bucket entries are normal cloud
 * nodes; [NodeSourceType.RECENTS_BUCKET] is only the navigation context (e.g. View in folder).
 */
class RecentsBucketMenuOptionsProvider @Inject constructor(
    @CloudDrive private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @CloudDrive private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.RECENTS_BUCKET
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions() = selectionModeOptions.get()
}
