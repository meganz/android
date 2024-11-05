package mega.privacy.android.feature.sync.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }

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