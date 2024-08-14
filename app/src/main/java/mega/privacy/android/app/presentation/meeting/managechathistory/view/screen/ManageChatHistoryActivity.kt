package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * This activity displays a screen allowing the user to manage the chat history. Options:
 * - The user can clear the chat history
 * - The user can set the retention time for the chat history
 */
@AndroidEntryPoint
class ManageChatHistoryActivity : FragmentActivity() {

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            Timber.e("Cannot init view, Intent is null")
            finish()
        }

        supportFragmentManager.commit {
            replace(android.R.id.content, ManageChatHistoryFragment().apply {
                arguments = intent.extras
            })
        }
    }
}
