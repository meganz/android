package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.Offline
import javax.inject.Inject

/**
 * Options provider for Offline source type
 *
 * Provides menu options for files viewed from the Offline section.
 * This enables the shared node options bottom sheet to work when
 * viewing offline files (e.g., PDFs opened from Offline).
 */
class OfflineMenuOptionsProvider @Inject constructor(
    @Offline private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @Offline private val selectionModeOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.OFFLINE
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions() = selectionModeOptions.get()
}
