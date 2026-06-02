package mega.privacy.android.app.presentation.meeting.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.chat.list.ChatTabsFragment
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.navigation.destination.ChatNavKey
import timber.log.Timber

/**
 * Host Activity for new chat room
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

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
        val chatId = intent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L)
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

    companion object {
        const val OPEN_CHAT_LIST = "open_chat_list"
        const val CREATE_NEW_CHAT = "create_new_chat"
    }
}
