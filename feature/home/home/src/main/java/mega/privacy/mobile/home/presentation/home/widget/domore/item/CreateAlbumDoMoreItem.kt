package mega.privacy.mobile.home.presentation.home.widget.domore.item

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem
import javax.inject.Inject

/**
 * "Create album" shortcut in the "Do more with MEGA" section.
 */
class CreateAlbumDoMoreItem @Inject constructor() : DoMoreWithMegaItem {
    override val identifier: DoMoreWithMegaItem.Identifier =
        DoMoreWithMegaItem.Identifier.CreateAlbum
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.RectangleImageStack
    override val labelRes: Int = sharedR.string.home_do_more_with_mega_create_album
    override val monitorVisibility: Flow<Boolean> = flowOf(true)
}
