package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.MyAccountHomeWidgetButtonPressedEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountHomeWidgetButtonPressedEvent
import javax.inject.Inject

/**
 * MyAccount widget for Home screen
 */
class MyAccountHomeWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "MyAccountWidgetProvider"
    override val defaultOrder: Int = 1
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.StringRes(R.string.section_my_account)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<MyAccountWidgetViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        MyAccountWidget(
            state = state,
            modifier = modifier,
            onClick = {
                navigationHandler.navigate(
                    when (state.storageQuotaLevel) {
                        QuotaLevel.Success -> {
                            Analytics.tracker.trackEvent(MyAccountHomeWidgetButtonPressedEvent)
                            MyAccountNavKey()
                        }

                        else -> {
                            Analytics.tracker.trackEvent(UpgradeAccountHomeWidgetButtonPressedEvent)
                            UpgradeAccountNavKey()
                        }
                    }
                )
            }
        )
    }
}
