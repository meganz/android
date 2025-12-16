package mega.privacy.android.app.presentation.meeting.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener
import mega.privacy.android.app.presentation.chat.list.ChatTabsFragment
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.JoinMeetingPressedEvent
import mega.privacy.mobile.analytics.event.StartMeetingNowPressedEvent
import timber.log.Timber

/**
 * Host Activity for new chat room
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity(), MeetingBottomSheetDialogActionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            val isOpenChatList = intent.getBooleanExtra(OPEN_CHAT_LIST, false)
            Timber.d("ChatHostActivity.onCreate: isOpenChatList=$isOpenChatList, intent.action=${intent.action}")
            supportFragmentManager.commit {
                if (isOpenChatList) {
                    replace(
                        android.R.id.content,
                        ChatTabsFragment().apply {
                            arguments = intent.extras
                        },
                    )
                } else {
                    replace(
                        android.R.id.content,
                        ChatFragment().apply {
                            arguments = intent.extras
                        },
                    )
                }
            }
        }
    }

    /**
     * Handle the intent to open chat room
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val chatId = intent.getLongExtra(Constants.CHAT_ID, -1L)
        val link = intent.getStringExtra(EXTRA_LINK)
        val action = intent.getStringExtra(EXTRA_ACTION)
        val isOpenChatList = intent.getBooleanExtra(OPEN_CHAT_LIST, false)
        if (isOpenChatList) {
            supportFragmentManager.commit {
                replace(
                    android.R.id.content,
                    ChatTabsFragment().apply {
                        arguments = intent.extras
                    },
                )
            }
        } else {
            openChatFragment(this, chatId = chatId, chatLink = link, action = action)
        }
    }

    override fun onJoinMeeting() {
        Analytics.tracker.trackEvent(JoinMeetingPressedEvent)
        if (CallUtil.participatingInACall()) {
            CallUtil.showConfirmationInACall(
                this,
                getString(sharedR.string.can_only_join_one_call_error_message),
            )
        } else {
            (supportFragmentManager.findFragmentById(android.R.id.content) as? ChatTabsFragment)
                ?.showOpenLinkDialog(true)
        }
    }

    override fun onCreateMeeting() {
        Analytics.tracker.trackEvent(StartMeetingNowPressedEvent)
        (supportFragmentManager.findFragmentById(android.R.id.content) as? ChatTabsFragment)
            ?.onCreateMeeting()
    }

    companion object {
        const val OPEN_CHAT_LIST = "open_chat_list"
        const val CREATE_NEW_CHAT = "create_new_chat"
    }
}
