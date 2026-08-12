package mega.privacy.android.app.menu.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Destination for [MenuItemPlaceholder]. The anchor is never rendered or navigated to, so this
 * key only exists to satisfy [NavDrawerItem.Account]'s contract.
 */
@Serializable
private data object MenuItemPlaceHolderNavKey : NavKey

/**
 * A non-visible placeholder bound into the menu items map. Its only purpose is to reserve a stable
 * key between the Chat and Device Centre rows: because Dagger enforces unique keys across an
 * `@IntoMap` binding, this reservation fails the build if another item is ever bound to the same
 * key. The dynamically-derived hidden-section rows are then ordered relative to this anchor's key
 * (see `MenuViewModel`), so they never overrun into the following static row.
 *
 * The item is filtered out before the menu is rendered; its icon and title are never shown.
 */
object MenuItemPlaceholder : NavDrawerItem.Account(
    destination = MenuItemPlaceHolderNavKey,
    icon = IconPack.Medium.Thin.Outline.Menu01,
    title = sharedR.string.general_menu,
)
