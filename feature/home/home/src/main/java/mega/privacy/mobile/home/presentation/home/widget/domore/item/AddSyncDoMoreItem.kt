package mega.privacy.mobile.home.presentation.home.widget.domore.item

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem
import javax.inject.Inject

/**
 * "Add sync" shortcut in the "Do more with MEGA" section.
 */
class AddSyncDoMoreItem @Inject constructor() : DoMoreWithMegaItem {
    override val identifier: DoMoreWithMegaItem.Identifier = DoMoreWithMegaItem.Identifier.AddSync
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Sync01
    override val labelRes: Int = sharedR.string.home_do_more_with_mega_add_sync
}
