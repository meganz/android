package mega.privacy.android.app.presentation.meeting.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.utils.Constants

/**
 * Host Activity for new chat room
 */
@AndroidEntryPoint
class ChatHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, ChatFragment().apply {
                    arguments = intent.extras
                })
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
        openChatFragment(this, chatId = chatId, chatLink = link)
    }
}
