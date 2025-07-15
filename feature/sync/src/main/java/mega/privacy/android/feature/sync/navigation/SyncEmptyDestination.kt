package mega.privacy.android.feature.sync.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent

/**
 * Route to the onboarding screen
 */
@Serializable
data object SyncEmptyRoute

internal fun NavGraphBuilder.syncEmptyDestination(
    onNavigateToNewFolder: () -> Unit,
) {
    composable<SyncEmptyRoute> {
        Analytics.tracker.trackEvent(AddSyncScreenEvent)
        SyncEmptyScreen {
            Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
            onNavigateToNewFolder()
        }
    }
}