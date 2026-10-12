package mega.privacy.mobile.home.presentation.home.widget.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.PwmBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferItBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.VpnBannerCloseButtonPressedEvent
import mega.privacy.mobile.home.presentation.home.rememberNoConnectionSnackbar
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper
import mega.privacy.mobile.home.presentation.home.widget.banner.view.ScrollableBanner
import javax.inject.Inject

/**
 * Banner widget that displays scrollable promotional banners on the Home screen
 */
class BannerWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "BannerWidgetProvider"
    override val defaultOrder: HomeWidgetOrder = HomeWidgetOrder.Banner
    override val canDelete: Boolean = false
    override val isConfigurable: Boolean = false
    override val isDraggable: Boolean = false
    override suspend fun getWidgetName() = LocalizedText.StringRes(sharedR.string.section_banners)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<BannerWidgetViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val showNoConnectionSnackbar = rememberNoConnectionSnackbar()

        if (uiState.offerBanner != null || uiState.banners.isNotEmpty()) {
            ScrollableBanner(
                offerBanner = uiState.offerBanner,
                banners = uiState.banners,
                onDismiss = { id, url ->
                    if (!uiState.isOfflineMode) {
                        if (BannerClickHandler.matchesPwmUrl(url)) {
                            Analytics.tracker.trackEvent(PwmBannerCloseButtonPressedEvent)
                        } else if (BannerClickHandler.matchesVpnUrl(url)) {
                            Analytics.tracker.trackEvent(VpnBannerCloseButtonPressedEvent)
                        } else if (url != SubscriptionOfferBannerMapper.SUBSCRIPTION_OFFER_BANNER_URL) {
                            Analytics.tracker.trackEvent(TransferItBannerCloseButtonPressedEvent)
                        }
                        viewModel.dismissBanner(id)
                    } else {
                        showNoConnectionSnackbar()
                    }
                },
                onClick = { url ->
                    if (!uiState.isOfflineMode) {
                        BannerClickHandler.handleBannerClick(
                            context = context,
                            navigationHandler = navigationHandler,
                            url = url,
                        )
                    } else {
                        showNoConnectionSnackbar()
                    }
                },
                modifier = modifier,
                isOfflineMode = uiState.isOfflineMode,
            )
        }
    }
}
