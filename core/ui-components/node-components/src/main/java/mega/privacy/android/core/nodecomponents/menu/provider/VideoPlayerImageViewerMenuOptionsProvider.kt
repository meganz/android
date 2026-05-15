package mega.privacy.android.core.nodecomponents.menu.provider

import dagger.Lazy
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.VideoPlayerImageViewer
import javax.inject.Inject

/**
 * Provides node bottom sheet menu options for the Video Player Image Viewer source type.
 */
class VideoPlayerImageViewerMenuOptionsProvider @Inject constructor(
    @VideoPlayerImageViewer private val bottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : NodeMenuOptionsProvider {
    override val supportedSourceType: NodeSourceType = NodeSourceType.VIDEO_PLAYER_IMAGE_VIEWER
    override fun getBottomSheetOptions() = bottomSheetOptions.get()
    override fun getSelectionModeOptions(): Set<@JvmSuppressWildcards NodeSelectionMenuItem<MenuActionWithIcon>> =
        emptySet()
}
