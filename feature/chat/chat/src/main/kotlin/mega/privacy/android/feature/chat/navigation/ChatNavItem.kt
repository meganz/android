package mega.privacy.android.feature.chat.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.DefaultNumberBadge
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.ChatRoomsBottomNavigationItemEvent
import timber.log.Timber

/**
 * Main navigation item for the Chat section.
 *
 * Has no default slot; it only appears in the navigation bar when the user adds it via the
 * navigation items preference. Flagged behind [ApiFeatures.CustomisableBottomNavigation], so it is
 * only surfaced when that flag is enabled.
 *
 * @property getNumUnreadChatsUseCase monitors the unread chat count used to drive the item badge.
 */
class ChatNavItem(
    private val getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase,
) : MainNavItem, Flagged {
    override val id: String = "chat"
    override val feature: Feature = ApiFeatures.CustomisableBottomNavigation
    override val destination: MainNavItemNavKey = ChatListNavKey(showMeetingTab = true)
    override val screen: EntryProviderScope<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, _, _ ->
            chatListScreen(navigationHandler = navigationHandler)
        }

    override val icon: ImageVector = IconPack.Medium.Thin.Outline.MessageChatCircle
    override val selectedIcon: ImageVector? = IconPack.Medium.Thin.Solid.MessageChatCircle
    override val badge: Flow<MainNavItemBadge?> =
        getNumUnreadChatsUseCase()
            .map { count -> DefaultNumberBadge(count).takeIf { count > 0 } }
            .catch { Timber.e(it) }

    @StringRes
    override val label: Int = sharedR.string.general_chat
    override val preferredSlot: PreferredSlot = PreferredSlot.None
    override val availableOffline: Boolean = false
    override val analyticsEventIdentifier: NavigationEventIdentifier =
        ChatRoomsBottomNavigationItemEvent
}
