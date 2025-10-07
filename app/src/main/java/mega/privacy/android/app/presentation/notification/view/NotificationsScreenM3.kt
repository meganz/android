package mega.privacy.android.app.presentation.notification.view

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.notification.NotificationViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import timber.log.Timber

@Composable
fun NotificationsScreenM3(
    navigationHandler: NavigationHandler,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    NotificationViewM3(
        state = uiState,
        onNotificationClick = { notification ->
            notification.destination?.let {
                navigationHandler.navigate(it)
            }
        },
        onPromoNotificationClick = { promoNotification ->
            runCatching {
                Intent(Intent.ACTION_VIEW, promoNotification.actionURL.toUri()).apply {
                    context.startActivity(this)
                }
            }.onFailure {
                Timber.e("No Application found that can handle promo notification intent")
            }
        },
        onNotificationsLoaded = {
            viewModel.onNotificationsLoaded()
        },
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        }
    )
}