package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VideoToPlaylistScreen
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * The activity for adding the video to video playlist
 */
@AndroidEntryPoint
class VideoToPlaylistActivity : ComponentActivity() {
    /**
     * [MonitorThemeModeUseCase] injection
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

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
        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(
                initialValue = ThemeMode.System
            )
            SessionContainer {
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            PsaContainer {
                                VideoToPlaylistScreen(
                                    viewModel = viewModel,
                                    addedVideoFinished = { titles ->
                                        val message = if (titles.isNotEmpty()) {
                                            resources.getQuantityString(
                                                sharedR.plurals.video_section_playlists_add_to_playlists_successfully_message,
                                                titles.size,
                                                if (titles.size == 1) titles.first() else titles.size
                                            )
                                        } else {
                                            resources.getString(sharedR.string.video_section_playlists_add_to_playlists_failed_message)
                                        }
                                        val videoHandle =
                                            this@VideoToPlaylistActivity.intent.getLongExtra(
                                                INTENT_EXTRA_KEY_HANDLE,
                                                -1
                                            )
                                        setResult(
                                            RESULT_OK,
                                            Intent().apply {
                                                putStringArrayListExtra(
                                                    INTENT_SUCCEED_ADDED_PLAYLIST_TITLES,
                                                    ArrayList(titles)
                                                )

                                                putExtra(INTENT_RESULT_MESSAGE, message)
                                                putExtra(INTENT_ADDED_VIDEO_HANDLE, videoHandle)
                                                putExtra(INTENT_RESULT_IS_RETRY, titles.isEmpty())
                                            }
                                        )
                                        finish()
                                    }
                                )
                            }
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

        /**
         * The intent key for added video handle
         */
        const val INTENT_ADDED_VIDEO_HANDLE = "INTENT_ADDED_VIDEO_HANDLE"

        /**
         * The intent key for result message
         */
        const val INTENT_RESULT_MESSAGE = "INTENT_RESULT_MESSAGE"


        /**
         * The intent key whether is retry
         */
        const val INTENT_RESULT_IS_RETRY = "INTENT_RESULT_IS_RETRY"
    }
}