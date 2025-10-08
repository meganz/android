package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.OutgoingShares
import javax.inject.Inject

/**
 * Options provider for Outgoing Shares source type
 */
class OutgoingSharesMenuOptionsProvider @Inject constructor(
    @OutgoingShares private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @OutgoingShares private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType = NodeSourceType.OUTGOING_SHARES
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions() = selectionModeOptions.get()
}
