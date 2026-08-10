package mega.privacy.android.shared.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.AdsFreeIntroNavKey
import mega.privacy.android.shared.ads.advertisements.AdsViewModel

/**
 * Container that displays a banner ad below [content].
 *
 * @param showAdsForScreen Whether ads are allowed for the current screen. Defaults to `true` for
 * callers with no extra constraint (e.g. the home screen). For file/folder link screens this should
 * be the per-link `QueryAdsUseCase` result, so a link created by a Pro user shows no ad.
 */
@Composable
fun NewAdsContainer(
    modifier: Modifier,
    onNavigate: (NavKey) -> Unit = {},
    showAdsForScreen: Boolean = true,
    viewModel: AdsViewModel = hiltViewModel(),
    content: @Composable ColumnScope.(Modifier) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(showAdsForScreen) {
        viewModel.setAdsAllowedForScreen(showAdsForScreen)
    }
    LifecycleResumeEffect(Unit) {
        viewModel.scheduleRefreshAds()

        onPauseOrDispose {
            viewModel.cancelRefreshAds()
        }
    }
    val isAdsShow = uiState.request != null
    val contentModifier = if (isAdsShow) {
        Modifier.consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
    } else {
        Modifier
    }
    Column(
        modifier = modifier
    ) {
        content(contentModifier)
        AdsContainer(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth(),
            request = uiState.request,
            onCloseAds = { onNavigate(AdsFreeIntroNavKey) },
        )
    }
}