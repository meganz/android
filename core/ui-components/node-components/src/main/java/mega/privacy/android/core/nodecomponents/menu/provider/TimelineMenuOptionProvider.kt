package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.Timeline
import javax.inject.Inject

class TimelineMenuOptionProvider @Inject constructor(
    @Timeline private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {

    override val supportedSourceType: NodeSourceType = NodeSourceType.TIMELINE

    override fun getBottomSheetOptions(): Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>> =
        emptySet()

    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        selectionModeOptions.get()
}
