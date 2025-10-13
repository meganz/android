package mega.privacy.android.app.presentation.notification.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.notification.view.NotificationsScreenM3
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.NotificationsNavKey

fun EntryProviderBuilder<NavKey>.notifications(navigationHandler: NavigationHandler) {
    entry<NotificationsNavKey> {
        NotificationsScreenM3(navigationHandler)
    }
}
