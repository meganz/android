package mega.privacy.mobile.home.presentation.home.widget.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.feature.home.R
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.mobile.home.presentation.home.widget.banner.view.ScrollableBanner
import javax.inject.Inject

/**
 * Banner widget that displays scrollable promotional banners on the Home screen
 */
class BannerWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "BannerWidgetProvider"
    override val defaultOrder: Int = 2
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.StringRes(R.string.section_banners)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<BannerWidgetViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        if (uiState.banners.isNotEmpty()) {
            ScrollableBanner(
                banners = uiState.banners,
                onDismiss = viewModel::dismissBanner,
                onClick = { url ->
                    BannerClickHandler.handleBannerClick(
                        context = context,
                        navigationHandler = navigationHandler,
                        url = url,
                    )
                },
                modifier = modifier,
            )
        }
    }
}
