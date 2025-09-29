package mega.privacy.android.app.presentation.notification.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.notification.view.NotificationsScreenM3
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.NotificationsNavKey

fun NavGraphBuilder.notifications(navigationHandler: NavigationHandler) {
    composable<NotificationsNavKey> {
        NotificationsScreenM3(navigationHandler)
    }
}
