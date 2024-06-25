package mega.privacy.android.app.presentation.contact.invite

import android.os.Bundle
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import timber.log.Timber

/**
 * This activity:
 * - Displays a screen allowing the user to invite new contacts.
 * - Will replace the [InviteContactActivity].
 * - Extends [BaseActivity] because we need to reference the PSA browser in onBackPressed.
 */
@AndroidEntryPoint
class InviteContactActivityV2 : BaseActivity() {

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.commit {
            replace(android.R.id.content, InviteContactFragment())
        }
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        finish()
    }
}
