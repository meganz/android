package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VideoToPlaylistScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * The activity for adding the video to video playlist
 */
@AndroidEntryPoint
class VideoToPlaylistActivity : ComponentActivity() {
    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * PasscodeCryptObjectFactory injection
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    private val viewModel by viewModels<VideoToPlaylistViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(
                initialValue = ThemeMode.System
            )
            SessionContainer {
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            VideoToPlaylistScreen(
                                viewModel = viewModel,
                                addedVideoFinished = { titles ->
                                    setResult(
                                        RESULT_OK,
                                        Intent().putStringArrayListExtra(
                                            INTENT_SUCCEED_ADDED_PLAYLIST_TITLES, ArrayList(titles)
                                        )
                                    )
                                    finish()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * The intent key for succeed added playlist titles
         */
        const val INTENT_SUCCEED_ADDED_PLAYLIST_TITLES = "INTENT_SUCCEED_ADDED_PLAYLIST_TITLES"
    }
}