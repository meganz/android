package mega.privacy.android.app.main.ads

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.advertisements.AdsViewModel

@Composable
fun NewAdsContainer(
    modifier: Modifier,
    viewModel: AdsViewModel = hiltViewModel(),
    content: @Composable ColumnScope.(Modifier) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        )
    }
}