package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.Chat
import javax.inject.Inject

/**
 * Provides node bottom sheet menu options for the Chat source type.
 */
class ChatMenuOptionsProvider @Inject constructor(
    @Chat private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.CHAT
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        emptySet()
}
