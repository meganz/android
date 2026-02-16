package mega.privacy.android.app.menu.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.myaccount.presentation.model.AvatarContent
import mega.privacy.android.navigation.contract.NavDrawerItem
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

data class MenuUiState(
    val myAccountItems: Map<Int, NavDrawerItem.Account> = emptyMap(),
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
)
