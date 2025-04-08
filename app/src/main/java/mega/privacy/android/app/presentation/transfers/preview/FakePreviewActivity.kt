package mega.privacy.android.app.presentation.transfers.preview

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.transfers.preview.FakePreviewFragment.Companion.EXTRA_TRANSFER_UNIQUE_ID

@AndroidEntryPoint
class FakePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, FakePreviewFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val transferUniqueId = getIntent()?.extras?.getLong(EXTRA_TRANSFER_UNIQUE_ID, -1)
            .takeUnless { it == -1L }

        supportFragmentManager.commit {
            replace(android.R.id.content, FakePreviewFragment().apply {
                intent.putExtra(EXTRA_TRANSFER_UNIQUE_ID, transferUniqueId)
                arguments = intent.extras
            })
        }
    }
}