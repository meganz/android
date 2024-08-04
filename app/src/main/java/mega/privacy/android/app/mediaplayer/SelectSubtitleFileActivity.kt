package mega.privacy.android.app.mediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.INTENT_KEY_SUBTITLE_FILE_INFO
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.AddSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.CancelSelectSubtitlePressedEvent
import javax.inject.Inject

/**
 * The activity for select subtitle file
 */
@AndroidEntryPoint
class SelectSubtitleFileActivity : PasscodeActivity() {
    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * [SelectSubtitleFileViewModel] injection
     */
    val viewModel by viewModels<SelectSubtitleFileViewModel>()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.searchState == SearchWidgetState.EXPANDED) {
                viewModel.closeSearch()
            } else {
                setResult(RESULT_CANCELED)
                this@SelectSubtitleFileActivity.finish()
            }
        }
    }

    /**
     * onCreate lifecycle function
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                SelectSubtitleComposeView(
                    viewModel = viewModel,
                    onAddSubtitle = { info ->
                        Analytics.tracker.trackEvent(AddSubtitlePressedEvent)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(
                                INTENT_KEY_SUBTITLE_FILE_INFO,
                                info
                            )
                        )
                        this.finish()
                    },
                    onBackPressed = {
                        Analytics.tracker.trackEvent(CancelSelectSubtitlePressedEvent)
                        setResult(RESULT_CANCELED)
                        this.finish()
                    })
            }
        }
    }
}