package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A single action item shown in the "Do more with MEGA" home section.
 *
 * Each item is contributed to a Dagger multibinding via `@IntoSet`, so the section
 * composes itself from whatever items are provided. This keeps every shortcut fully
 * decoupled: adding or removing one is just a matter of adding/removing an `@IntoSet`
 * binding, with no changes to the widget itself.
 *
 * Items are pure descriptors (icon + label + identifier); the click action is handled by the
 * caller, which owns the navigation context. Items are rendered in [Identifier] declaration order.
 */
interface DoMoreWithMegaItem {

    /**
     * Uniquely identifies the shortcut. The caller uses it to decide what tapping the item does,
     * and its [Identifier] declaration order also determines where the item appears in the row.
     */
    val identifier: Identifier

    /**
     * Icon shown inside the circular button.
     */
    val icon: ImageVector

    /**
     * String resource for the label shown below the icon.
     */
    @get:StringRes
    val labelRes: Int

    /**
     * The set of shortcuts in the "Do more with MEGA" section. Earlier-declared entries appear
     * earlier in the row (their [ordinal] is used as the sort key), so reordering the section is a
     * single, conflict-free change here.
     */
    enum class Identifier {
        CameraUploads,
        AddSync,
        ScanDocument,
        CreateAlbum,
        AddContact,
        ScheduleMeeting,
    }
}
