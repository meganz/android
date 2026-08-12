package mega.privacy.android.app.menu.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.feature.myaccount.presentation.model.AvatarContent
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.home.presentation.home.widget.banner.model.SubscriptionOfferBannerUiModel

data class MenuUiState(
    val myAccountItems: ImmutableList<NavDrawerItem.Account> = persistentListOf(),
    val privacySuiteItems: Map<Int, NavDrawerItem.PrivacySuite> = emptyMap(),
    val name: String? = null,
    val email: String? = null,
    val avatarContent: AvatarContent? = null,
    val lastModifiedTime: Long = 0L,
    val isConnectedToNetwork: Boolean = true,
    val showTestPasswordScreenEvent: StateEvent = consumed,
    val showLogoutConfirmationEvent: StateEvent = consumed,
    val isLoggingOut: Boolean = false,
    val unreadNotificationsCount: Int = 0,
    val analyticsEventIdentifier: NavigationEventIdentifier? = null,
    val offerBanner: SubscriptionOfferBannerUiModel? = null,
)
