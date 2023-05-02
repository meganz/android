package mega.privacy.android.app.mediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerFragment.Companion.INTENT_KEY_SUBTITLE_FILE_INFO
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.controls.SearchWidgetState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
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
                setResult(RESULT_OK)
                this@SelectSubtitleFileActivity.finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                SelectSubtitleComposeView(
                    onAddSubtitle = { info ->
                        info?.let {
                            viewModel.sendAddSubtitleClickedEvent()
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(
                                    INTENT_KEY_SUBTITLE_FILE_INFO,
                                    it
                                )
                            )
                            this.finish()
                        }
                    },
                    onBackPressed = {
                        viewModel.sendSelectSubtitleCancelledEvent()
                        setResult(RESULT_OK)
                        this.finish()
                    })
            }
        }
    }
}