package mega.privacy.mobile.home.presentation.home.widget.domore.item

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem
import javax.inject.Inject

/**
 * "Add contact" shortcut in the "Do more with MEGA" section.
 */
class AddContactDoMoreItem @Inject constructor() : DoMoreWithMegaItem {
    override val identifier: DoMoreWithMegaItem.Identifier =
        DoMoreWithMegaItem.Identifier.AddContact
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.UserPlus
    override val labelRes: Int = sharedR.string.home_do_more_with_mega_add_contact
    override val monitorVisibility: Flow<Boolean> = flowOf(true)
}
