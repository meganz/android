package mega.privacy.android.app.activities.destinations

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.notification.view.NotificationsScreenM3
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.Notifications

fun NavGraphBuilder.notifications(navigationHandler: NavigationHandler) {
    composable<Notifications> {
        NotificationsScreenM3(navigationHandler)
    }
}
