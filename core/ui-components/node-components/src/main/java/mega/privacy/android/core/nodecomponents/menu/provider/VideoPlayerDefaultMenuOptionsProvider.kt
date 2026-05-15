package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.VideoPlayerDefault
import javax.inject.Inject

/**
 * Provides node bottom sheet menu options for the Video Player Default source type.
 */
class VideoPlayerDefaultMenuOptionsProvider @Inject constructor(
    @VideoPlayerDefault private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.VIDEO_PLAYER_DEFAULT
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        emptySet()
}
