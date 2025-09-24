package mega.privacy.android.app.presentation.notification.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.notification.NotificationViewModel
import mega.privacy.android.navigation.contract.NavigationHandler

@Composable
fun NotificationsScreenM3(
    navigationHandler: NavigationHandler,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    NotificationViewM3(
        state = uiState,
        onNotificationClick = { notification ->
            notification.destination?.let {
                navigationHandler.navigate(it)
            }
        },
        onPromoNotificationClick = { promoNotification ->
            //todo handle promoNotification click
        },
        onNotificationsLoaded = {
            viewModel.onNotificationsLoaded()
        },
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        }
    )
}