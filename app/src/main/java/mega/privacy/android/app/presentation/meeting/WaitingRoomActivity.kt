package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.view.WaitingRoomView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.core.ui.theme.AndroidTheme
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject


/**
 * Activity which shows waiting room.
 */
@AndroidEntryPoint
class WaitingRoomActivity : AppCompatActivity() {

    companion object {
        internal const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
        internal const val EXTRA_CHAT_LINK = "EXTRA_CHAT_LINK"

        private const val INFO_SCREEN_URL = "https://mega.io/chatandmeetings"
        private const val INVALID_HANDLE = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    }

    @Inject
    internal lateinit var passCodeFacade: PasscodeCheck

    private val viewModel by viewModels<WaitingRoomViewModel>()

    private val chatId: Long? by lazy {
        intent.getLongExtra(EXTRA_CHAT_ID, INVALID_HANDLE).takeIf { it != INVALID_HANDLE }
    }

    private val chatPublicLink: String? by lazy {
        intent.getStringExtra(EXTRA_CHAT_LINK)
    }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainComposeView() }

        collectFlow(viewModel.state) { uiState ->
            when {
                uiState.joinCall ->
                    launchCallScreen(
                        chatId = uiState.chatId,
                        chatLink = uiState.chatLink,
                        micEnabled = uiState.micEnabled,
                        cameraEnabled = uiState.cameraEnabled,
                        guestFirstName = uiState.guestFirstName,
                        guestLastName = uiState.guestLastName,
                    )

                uiState.finish ->
                    finish()
            }
        }

        if (savedInstanceState == null) {
            viewModel.loadMeetingDetails(chatId, chatPublicLink)
        }
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
                onCloseClicked = viewModel::finishWaitingRoom,
                onMicToggleChange = viewModel::enableMicrophone,
                onCameraToggleChange = viewModel::enableCamera,
                onSpeakerToggleChange = viewModel::enableSpeaker,
                onGuestNameChange = viewModel::setGuestName,
                videoStream = viewModel.getVideoStream(),
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

    private fun launchCallScreen(
        chatId: Long,
        chatLink: String?,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
        guestFirstName: String?,
        guestLastName: String?,
    ) {
        val intent = Intent(this, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MeetingActivity.MEETING_ACTION_IN
            putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
            putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, micEnabled)
            putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, cameraEnabled)
            chatLink?.takeIf(String::isNotBlank)?.let { link ->
                putExtra(MeetingActivity.MEETING_IS_GUEST, true)
                putExtra(MeetingActivity.MEETING_GUEST_FIRST_NAME, guestFirstName)
                putExtra(MeetingActivity.MEETING_GUEST_LAST_NAME, guestLastName)
                data = link.toUri()
            }
        }
        startActivity(intent)
        finish()
    }
}
