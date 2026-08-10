package mega.privacy.android.app.presentation.videoplayer.navigation

import android.content.res.Configuration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerSelectSubtitleViewModel
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerSelectSubtitleView
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent

/**
 * Select subtitle NavKey for the revamped video player.
 */
@Serializable
internal data object VideoPlayerSelectSubtitleScreenNavKey : NavKey

internal fun EntryProviderScope<NavKey>.videoPlayerSelectSubtitleScreen(
    viewModel: VideoPlayerViewModelV2,
    onBack: () -> Unit,
) {
    entry<VideoPlayerSelectSubtitleScreenNavKey> {
        val context = LocalContext.current
        val isDark = remember(context) {
            (context.applicationContext.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        val systemUiController = rememberSystemUiController()
        val selectSubtitleViewModel = hiltViewModel<VideoPlayerSelectSubtitleViewModel>()
        val uiState by selectSubtitleViewModel.uiState.collectAsStateWithLifecycle()

        DisposableEffect(isDark) {
            systemUiController.isSystemBarsVisible = true
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !isDark,
            )
            onDispose {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = false,
                )
            }
        }

        OriginalTheme(isDark = isDark) {
            VideoPlayerSelectSubtitleView(
                uiState = uiState,
                onLoadSubtitleList = selectSubtitleViewModel::getSubtitleFileInfoList,
                onSearchTextChange = selectSubtitleViewModel::searchQuery,
                onItemClicked = selectSubtitleViewModel::itemClickedUpdate,
                onClearSelectedItem = selectSubtitleViewModel::clearSelectedItem,
                onAddSubtitle = { info ->
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = false,
                    )
                    Analytics.tracker.trackEvent(AddSubtitlePressedEvent)
                    viewModel.updateSubtitleSelectedStatus(
                        SubtitleSelectedStatus.AddSubtitleItem,
                        info
                    )
                    selectSubtitleViewModel.clearSelectedItem()
                    onBack()
                },
                onBackPressed = {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = false,
                    )
                    Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                    onBack()
                    viewModel.updateShowSubtitleDialog(false)
                }
            )
        }
    }
}
