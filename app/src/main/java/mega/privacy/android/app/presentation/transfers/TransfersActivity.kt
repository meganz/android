package mega.privacy.android.app.presentation.transfers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit

/**
 * Activity to show transfers.
 */
class TransfersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, TransfersFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }
}