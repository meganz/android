package mega.privacy.android.app.presentation.contact.invite

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import timber.log.Timber

/**
 * This activity:
 * - Displays a screen allowing the user to invite new contacts.
 * - Extends [BaseActivity] because we need to reference the PSA browser in onBackPressed.
 */
@AndroidEntryPoint
class InviteContactActivity : BaseActivity() {

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Timber.d("onBackPressed")
                val psaWebBrowser = psaWebBrowser
                if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
                finish()
            }
        })

        supportFragmentManager.commit {
            replace(android.R.id.content, InviteContactFragment())
        }
    }
}
