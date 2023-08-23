package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.meeting.view.WaitingRoomView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject


/**
 * Activity which shows waiting room.
 *
 * @property getThemeMode               [GetThemeMode]
 */
@AndroidEntryPoint
class WaitingRoomActivity : PasscodeActivity() {

    companion object {
        private const val INFO_SCREEN_URL = "https://mega.io/chatandmeetings"

        internal const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<WaitingRoomViewModel>()

    private val chatId by lazy {
        intent.getLongExtra(EXTRA_CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
    }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainComposeView() }

        collectFlow(viewModel.state) { uiState ->
            if (uiState.finish) finish()
        }

        viewModel.loadMeetingDetails(chatId)
    }

    /**
     * Open compose view
     */
    @Composable
    fun MainComposeView() {
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        AndroidTheme(isDark = true) {
            WaitingRoomView(
                state = uiState,
                onInfoClicked = ::launchInfoScreen,
                onCloseClicked = ::finish,
                onMicToggleChange = viewModel::onMicEnabled,
                onCameraToggleChange = viewModel::onCameraEnabled,
                onSpeakerToggleChange = viewModel::onSpeakerEnabled,
            )
        }
    }

    private fun launchInfoScreen() {
        val intent = Intent(Intent.ACTION_VIEW, INFO_SCREEN_URL.toUri())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Timber.w("Internet Browser not available")
        }
    }
}
