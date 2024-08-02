package mega.privacy.android.app.presentation.transfers

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity to show transfers.
 */

@AndroidEntryPoint
class TransfersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, TransfersFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }
}