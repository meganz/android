package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.RubbishBin
import javax.inject.Inject

/**
 * Options provider for Rubbish Bin source type
 */
class RubbishBinMenuOptionsProvider @Inject constructor(
    @RubbishBin private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @RubbishBin private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.RUBBISH_BIN
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions() = selectionModeOptions.get()
}
