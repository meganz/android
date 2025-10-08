package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.IncomingShares
import javax.inject.Inject

/**
 * Options provider for Incoming Shares source type
 */
class IncomingSharesMenuOptionsProvider @Inject constructor(
    @IncomingShares private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @IncomingShares private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.INCOMING_SHARES
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        selectionModeOptions.get()
}
