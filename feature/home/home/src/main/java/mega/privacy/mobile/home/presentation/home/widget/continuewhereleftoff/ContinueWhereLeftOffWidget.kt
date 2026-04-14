package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

class ContinueWhereLeftOffWidget @Inject constructor() : HomeWidget {
    override val identifier: String = "ContinueWhereLeftOffWidget"
    override val defaultOrder: Int = 4
    override val canDelete: Boolean = true

    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_widget_continue_where_left_off)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        Timber.d("CWLO: DisplayWidget called")
        FeatureFlagGate(
            feature = ApiFeatures.ContinueWhereLeftOff,
            disabled = {
                Timber.d("CWLO: Feature flag is DISABLED")
            },
        ) {
            Timber.d("CWLO: Feature flag is ENABLED")
            val viewModel: ContinueWhereLeftOffViewModel = hiltViewModel()
            val items by viewModel.items.collectAsStateWithLifecycle()

            Timber.d("CWLO: items count = ${items.size}")
            ContinueWhereLeftOffCarousel(
                items = items,
                onItemClick = { },
                modifier = modifier,
            )
        }
    }
}
