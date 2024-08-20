package mega.privacy.android.feature.sync.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity to show the Sync feature by hosting the [SyncFragment]
 */
@AndroidEntryPoint
class SyncHostActivity : AppCompatActivity() {

    /**
     * Initializes the activity.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, SyncFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    /**
     * Handle the intent to open Sync
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supportFragmentManager.commit {
            replace(android.R.id.content, SyncFragment::class.java, intent.extras)
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
}