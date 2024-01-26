package mega.privacy.android.app.presentation.meeting.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint

/**
 * Host Activity for new chat room
 */
@AndroidEntryPoint
class ChatHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, ChatFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }
}