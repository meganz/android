package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.MediaPlayerVersions
import javax.inject.Inject

/**
 * Provides node bottom sheet menu options for the Media Player Versions source type.
 */
class MediaPlayerVersionsMenuOptionsProvider @Inject constructor(
    @MediaPlayerVersions private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.MEDIA_PLAYER_VERSIONS
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        emptySet()
}
